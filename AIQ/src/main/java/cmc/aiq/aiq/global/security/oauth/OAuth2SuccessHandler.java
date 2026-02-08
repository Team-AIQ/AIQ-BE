package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.repository.UsersRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
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
        // 1. 쿠키에서 origin 값 추출 (파라미터는 유실되므로 쿠키 사용)
        String origin = "web"; // 기본값
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("login_origin".equals(cookie.getName())) {
                    origin = cookie.getValue();
                    // 사용한 쿠키는 삭제 (선택 사항)
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }

        // 2. origin에 따른 Base URL 결정
        String baseUrl = "app".equals(origin)
                ? "aiq://oauth/callback"           // iOS ReactNative 딥링크
                : "http://localhost:3000/oauth/callback"; // Web용

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = authToken.getAuthorizedClientRegistrationId();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 실제 운영 시에는 소셜별 이메일 추출 로직을 거쳐야 함
        String email = extractEmail(oAuth2User , registrationId);
        String providerId = extractProviderId(oAuth2User, registrationId);

        Users user = usersRepository.findByProviderIdAndEmail(providerId,email).orElseThrow();
        boolean isRememberMe = true;

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name() , isRememberMe);

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        // 프론트엔드(Next.js 등)로 토큰을 전달하며 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

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
}
