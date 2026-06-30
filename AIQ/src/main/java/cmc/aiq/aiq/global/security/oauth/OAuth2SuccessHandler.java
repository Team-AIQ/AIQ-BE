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

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // CustomOAuth2UserService에서 저장/업데이트한 User 정보를 가져옵니다.
        // provider와 providerId를 사용하여 DB에서 사용자를 다시 조회합니다.
        String registrationId = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String providerId = oAuth2User.getName(); // Principal의 name이 providerId가 됩니다.

        Users user = usersRepository.findByProviderAndProviderId(
                cmc.aiq.aiq.domain.ENUM.AuthProvider.valueOf(registrationId.toUpperCase()), providerId)
                .orElseThrow(() -> new RuntimeException("OAuth2 인증 후 회원 정보를 찾을 수 없습니다."));

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
            usersRepository.save(user);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name(), true);
        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        String origin = "web";
        if (request.getParameter("state") != null && request.getParameter("state").contains("origin=app")) {
            origin = "app";
        }

        String targetUrl;
        if ("app".equalsIgnoreCase(origin)) {
            // [수정] 프론트엔드의 REDIRECT_URL과 일치하도록, 커스텀 스킴으로 직접 리다이렉트합니다.
            targetUrl = UriComponentsBuilder.fromUriString("aiq://oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("앱으로 리다이렉트: {}", targetUrl);
        } else {
            // 웹일 경우, 프론트엔드 웹사이트의 특정 페이지로 리다이렉트
            targetUrl = UriComponentsBuilder.fromUriString("https://www.aiq.ai.kr/login-success")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
            log.info("웹으로 리다이렉트: {}", targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
