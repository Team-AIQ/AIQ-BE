package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersRepository usersRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 세션에서 origin 읽기 (OAuthOriginCookieFilter에서 저장)
        String origin = null;
        String redirectUriFromSession = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            origin = (String) session.getAttribute("oauth_origin");
            redirectUriFromSession = (String) session.getAttribute("oauth_redirect_uri");
            session.removeAttribute("oauth_origin");
            session.removeAttribute("oauth_redirect_uri");
        }
        System.out.println("origin = " + origin);
        System.out.println("redirectUriFromSession = " + redirectUriFromSession);

        // 기본값을 Expo Go 딥링크 콜백으로 설정
        String baseUrl = "exp://192.168.0.12:8082/--/oauth_callback";

        if (redirectUriFromSession != null && !redirectUriFromSession.isBlank()) {
            baseUrl = redirectUriFromSession;
        }

        if (redirectUriFromSession == null && "web".equals(origin)) {
            baseUrl = "https://aiq.ai.kr/oauth/callback";
        } else if (redirectUriFromSession == null && "app".equals(origin)) {
            baseUrl = "exp://192.168.0.12:8082/--/oauth_callback";  // ← 경로 추가
        }

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = authToken.getAuthorizedClientRegistrationId();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 실제 운영 시에는 소셜별 이메일 추출 로직을 거쳐야 함
        String email = extractEmail(oAuth2User , registrationId);
        String providerId = extractProviderId(oAuth2User, registrationId);

        Users user = usersRepository.findByProviderIdAndEmail(providerId,email)
                .orElseGet(() -> usersRepository.save(Users.builder()
                        .email(email)
                        .nickname(extractNickname(oAuth2User, registrationId))
                        .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                        .providerId(providerId)
                        .initialLoginAt(LocalDateTime.now())
                        .currentCredits(20L)
                        .build()));
        boolean isRememberMe = true;

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name() , isRememberMe);

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        // 프론트엔드(Next.js 등)로 토큰을 전달하며 리다이렉트
        String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        String targetUrl = baseUrl + "?accessToken=" + encodedAccessToken + "&refreshToken=" + encodedRefreshToken;

        System.out.println("baseUrl = " + baseUrl);
        System.out.println("accessToken length = " + (accessToken == null ? 0 : accessToken.length()));
        System.out.println("refreshToken length = " + (refreshToken == null ? 0 : refreshToken.length()));
        System.out.println("targetUrl = " + targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if ("google".equals(registrationId)) {
            // 구글은 "sub" 키에 고유 ID가 들어있습니다.
            return (String) attributes.get("sub");
        }

        if ("naver".equals(registrationId)) {
            // 네이버는 response 맵 안에 "id"가 들어있습니다.
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return (String) response.get("id");
        }

        if ("kakao".equals(registrationId)) {
            // 카카오는 최상위에 Long 타입의 "id"가 있습니다. String으로 변환하여 반환합니다.
            return attributes.get("id").toString();
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }

    private String extractEmail(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        }

        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return (String) response.get("email");
        }

        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return (String) kakaoAccount.get("email");
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }

    private String extractNickname(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        }

        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return (String) response.get("nickname");
        }

        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            return (String) profile.get("nickname");
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }
}