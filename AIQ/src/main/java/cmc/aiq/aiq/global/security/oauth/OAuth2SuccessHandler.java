package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.repository.UsersRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Log4j2
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersRepository usersRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // [수정] CustomOAuth2UserService가 처리한 결과를 Authentication 객체에서 안전하게 가져옵니다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        // [수정] DB를 직접 조회하는 대신, 이메일을 기반으로 사용자를 다시 조회하여 최신 상태를 가져옵니다.
        // CustomOAuth2UserService에서 이미 회원가입/업데이트가 완료된 상태입니다.
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("인증 후 DB에서 회원 정보를 찾을 수 없습니다. email: " + email));

        // 리프레시 토큰 만료일 갱신 로직 (기존 유지)
        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        // 토큰 생성 및 저장 (기존 유지)
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name(), true);
        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        // 리다이렉트 URL 분기 처리 (기존 유지)
        String origin = "web";
        if (request.getParameter("state") != null && request.getParameter("state").contains("origin=app")) {
            origin = "app";
        }

        String targetUrl;
        if ("app".equalsIgnoreCase(origin)) {
            targetUrl = UriComponentsBuilder.fromUriString("aiq://oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("앱으로 리다이렉트: {}", targetUrl);
        } else {
            targetUrl = UriComponentsBuilder.fromUriString("https://www.aiq.ai.kr/login-success")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("웹으로 리다이렉트: {}", targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
