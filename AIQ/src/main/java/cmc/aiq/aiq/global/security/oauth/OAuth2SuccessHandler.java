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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String registrationId = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        // [복원] 소셜 서비스 응답에서 이메일을 추출합니다.
        String email = extractEmail(attributes, registrationId);

        // [복원] 이메일을 기반으로 DB에서 사용자를 다시 조회합니다.
        // 이 시점에는 CustomOAuth2UserService에 의해 사용자가 반드시 DB에 존재합니다.
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("인증 후 DB에서 회원 정보를 찾을 수 없습니다. email: " + email));

        // 리프레시 토큰 만료일 갱신 로직
        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        // 토큰 생성 및 저장
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name(), true);
        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        // 리다이렉트 URL 분기 처리
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

    // [추가] 이메일 추출을 위한 헬퍼 메소드
    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("email");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("email");
            case "kakao" -> (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}
