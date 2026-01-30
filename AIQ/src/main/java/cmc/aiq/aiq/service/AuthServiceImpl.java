package cmc.aiq.aiq.service;

import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.domain.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.repository.UsersRepository;
import cmc.aiq.aiq.service.Mail.MailService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UsersRepository usersRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void signUp(SignUpRequestDTO request){
        if(usersRepository.existsByEmail(request.getEmail())) throw new RuntimeException("이미 존재하는 이메일입니다.");

        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .provider(AuthProvider.EMAIL)
                .currentCredits(50L)
                .build();
        usersRepository.save(user);
    }

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO loginrequestDTO) {
        Users user = usersRepository.findByEmail(loginrequestDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(loginrequestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now()); // 지성님이 만드신 메서드 사용!
        }
        // 두 개의 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(),  user.getRole().name() , loginrequestDTO.isRememberMe());

        // DB에 Refresh Token 업데이트
        user.updateRefreshToken(refreshToken);

        return new TokenResponseDTO(accessToken, refreshToken);
    }

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
        }

        // 2. DB에 저장된 토큰과 일치하는지 확인
        String email = jwtTokenProvider.getUserEmail(refreshToken);
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!user.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 3. 새로운 Access Token 발행
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail() ,  user.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponseDTO refresh(String refreshToken) {
        // 1. 토큰 유효성 검사 (JwtTokenProvider에 있는 메서드 활용)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 토큰에서 사용자 이메일 추출
        String email = jwtTokenProvider.getUserEmail(refreshToken);

        // 3. DB에서 사용자 조회
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 4. DB에 저장된 리프레시 토큰과 일치하는지 확인
        if (!refreshToken.equals(user.getRefreshToken())) {
            // 일치하지 않으면 누군가 예전 토큰을 쓴 것이므로 보안을 위해 토큰을 무효화합니다.
            user.updateRefreshToken(null);
            throw new RuntimeException("로그인 정보가 일치하지 않습니다. 다시 로그인해주세요.");
        }

        // 5. 절대 만료 시간(90일) 검증
        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateRefreshToken(null); // 세션 강제 종료
            throw new RuntimeException("보안 정책상 다시 로그인이 필요합니다. (절대 만료 기간 초과)");
        }

        boolean isRememberMe = jwtTokenProvider.getIsRememberMe(refreshToken);
        // 5. 새로운 토큰 쌍 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(),  user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail() ,  user.getRole().name(), isRememberMe);

        // 6. DB의 리프레시 토큰 업데이트 (Rotation 전략)
        user.updateRefreshToken(newRefreshToken);

        return new TokenResponseDTO(newAccessToken, newRefreshToken);
    }

    // 비밀번호 재설정 인증코드 생성 로직
    @Transactional
    public void sendResetCode(String email) throws MessagingException {
        // 1. 유저 확인 (Repository 사용)
        usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        // 2. 인증 코드 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 3. Redis 저장 (5분 유효)
        redisTemplate.opsForValue().set("PWD_CODE:" + email, code, Duration.ofMinutes(5));

        // 4. 메일 발송 의뢰 (MailService 호출)
        mailService.sendVerificationCode(email, code);
    }

    // 비밀번호 재설정 인증코드 대조 로직
    public String verifyResetCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("PWD_CODE:" + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않거나 만료되었습니다.");
        }

        // 인증 성공 시, 다음 단계(비밀번호 변경)를 위한 임시 토큰 발행
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("RESET_TOKEN:" + resetToken, email, Duration.ofMinutes(3));

        return resetToken;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        // 1. 임시 토큰으로 이메일 추출
        String email = redisTemplate.opsForValue().get("RESET_TOKEN:" + resetToken);
        if (email == null) {
            throw new RuntimeException("유효하지 않은 접근입니다.");
        }

        // 2. 실제 비밀번호 변경
        Users user = usersRepository.findByEmail(email).orElseThrow();
        user.updatePassword(passwordEncoder.encode(newPassword));

        // 3. 사용 완료된 토큰 삭제
        redisTemplate.delete("RESET_TOKEN:" + resetToken);
    }
}
