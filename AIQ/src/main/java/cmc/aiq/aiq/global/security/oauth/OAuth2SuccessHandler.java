package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.repository.UsersRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
        
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String email = extractEmail(attributes, registrationId);

        Users user = usersRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new RuntimeException("인증 후 DB에서 회원 정보를 찾을 수 없습니다. email: " + email + ", provider: " + provider));

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name(), true);
        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        // [수정] 'state' 파라미터 확인 로직을 삭제하고, 'origin' 파라미터만으로 앱/웹을 구분합니다.
        String origin = request.getParameter("origin");

        String targetUrl;
        if ("app".equalsIgnoreCase(origin)) {
            targetUrl = UriComponentsBuilder.fromUriString("aiq://oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("앱으로 리다이렉트: {}", targetUrl);
        } else {
            // origin 파라미터가 없거나 "app"이 아닌 모든 경우를 웹으로 간주합니다.
            targetUrl = UriComponentsBuilder.fromUriString("https://aiq.ai.kr/oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("웹으로 리다이렉트: {}", targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("email");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("email");
            case "kakao" -> (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}
