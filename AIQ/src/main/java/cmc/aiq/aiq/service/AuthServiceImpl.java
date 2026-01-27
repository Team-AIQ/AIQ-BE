package cmc.aiq.aiq.service;

import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.domain.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signUp(String email, String password, String nickname){
        if(userRepository.existsByEmail(email)) throw new RuntimeException("이미 존재하는 이메일입니다.");

        Users user = Users.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .provider(AuthProvider.EMAIL)
                .currentCredits(50L)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO loginrequestDTO) {
        Users user = userRepository.findByEmail(loginrequestDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(loginrequestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 두 개의 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

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
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!user.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 3. 새로운 Access Token 발행
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
    }
}
