package cmc.aiq.aiq.service;

import cmc.aiq.aiq.domain.ENUM.UserRole;
import cmc.aiq.aiq.dto.ChangePasswordRequestDTO;
import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.repository.UsersRepository;
import cmc.aiq.aiq.service.Mail.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- 올바른 어노테이션으로 수정

import java.security.SecureRandom;
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

    @Override
    @Transactional
    public void signUp(SignUpRequestDTO request){
        if(usersRepository.existsByEmailAndProvider(request.getEmail(), AuthProvider.EMAIL)) throw new RuntimeException("이미 존재하는 이메일입니다.");

        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .provider(AuthProvider.EMAIL)
                .currentCredits(20L)
                .build();
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public TokenResponseDTO login(LoginRequestDTO loginrequestDTO) {

        Users user = usersRepository.findByEmailAndProvider(loginrequestDTO.getEmail(),AuthProvider.EMAIL)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(loginrequestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }
        // 두 개의 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name() , user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(),  user.getRole().name() , loginrequestDTO.isRememberMe());

        user.updateRefreshToken(refreshToken);

        return new TokenResponseDTO(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponseDTO refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            user.updateRefreshToken(null);
            throw new RuntimeException("로그인 정보가 일치하지 않습니다. 다시 로그인해주세요.");
        }

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateRefreshToken(null);
            throw new RuntimeException("보안 정책상 다시 로그인이 필요합니다. (절대 만료 기간 초과)");
        }

        boolean isRememberMe = jwtTokenProvider.getIsRememberMe(refreshToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(),  user.getRole().name() , user.getNickname());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail() ,  user.getRole().name(), isRememberMe);

        user.updateRefreshToken(newRefreshToken);

        return new TokenResponseDTO(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void sendResetCode(String email) throws MessagingException {
        usersRepository.findByEmailAndProvider(email , AuthProvider.EMAIL)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        SecureRandom secureRandom = new SecureRandom();
        int number = secureRandom.nextInt(1000000);
        String code = String.format("%06d", number);

        String redisKey = "PWD_CODE:" + email;
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));

        try {
            mailService.sendVerificationCode(email, code);
            log.info("비밀번호 재설정 메일 발송 요청 성공: email={}, code={}", email, code);
        } catch (MessagingException e) {
            log.error("메일 발송 중 오류 발생: {}", e.getMessage());
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    @Override
    public String verifyResetCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("PWD_CODE:" + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않거나 만료되었습니다.");
        }

        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("RESET_TOKEN:" + resetToken, email, Duration.ofMinutes(3));

        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        String email = redisTemplate.opsForValue().get("RESET_TOKEN:" + resetToken);
        if (email == null) {
            throw new RuntimeException("유효하지 않은 접근입니다.");
        }

        Users user = usersRepository.findByEmailAndProvider(email , AuthProvider.EMAIL)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));
        user.updatePassword(passwordEncoder.encode(newPassword));

        redisTemplate.delete("RESET_TOKEN:" + resetToken);
    }

    @Override
    @Transactional
    public TokenResponseDTO loginAsGuest() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        Users guestUser = Users.builder()
                .email("guest_" + uuid + "@aiq.com")
                .nickname("게스트_" + uuid)
                .provider(AuthProvider.GUEST)
                .currentCredits(0L)
                .build();
        usersRepository.save(guestUser);

        String accessToken = jwtTokenProvider.createAccessToken(
                guestUser.getId(), guestUser.getEmail(), UserRole.ROLE_GUEST.name(), guestUser.getNickname()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                guestUser.getId(), guestUser.getEmail(), UserRole.ROLE_GUEST.name(), false
        );
        guestUser.updateRefreshToken(refreshToken);

        return new TokenResponseDTO(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void withdrawUser(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.withdraw();
        usersRepository.save(user);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("새로운 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        usersRepository.save(user);

        log.info("비밀번호 변경 완료: userId={}", userId);
    }
}
