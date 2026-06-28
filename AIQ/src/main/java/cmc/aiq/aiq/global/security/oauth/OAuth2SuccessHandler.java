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
        String email = (String) oAuth2User.getAttributes().get("email");
        String provider = request.getServletPath().substring(request.getServletPath().lastIndexOf('/') + 1);

        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        if (user.getInitialLoginAt() == null ||
                user.getInitialLoginAt().plusDays(90).isBefore(LocalDateTime.now())) {
            user.updateInitialLoginAt(LocalDateTime.now());
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getNickname());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), user.getRole().name(), true);
        user.updateRefreshToken(refreshToken);
        usersRepository.save(user);

        String origin = "web"; // 기본값은 웹
        if (request.getParameter("state") != null) {
            String state = request.getParameter("state");
            if (state.contains("origin=app")) {
                origin = "app";
            }
        }

        String targetUrl;
        if ("app".equalsIgnoreCase(origin)) {
            // [수정] 앱일 경우, 커스텀 스킴으로 직접 리다이렉트하는 대신, 토큰을 전달하는 웹 페이지로 리다이렉트
            targetUrl = UriComponentsBuilder.fromUriString("https://api.aiq.ai.kr/oauth/app/redirect")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
        } else {
            // 웹일 경우, 프론트엔드의 특정 페이지로 리다이렉트
            targetUrl = UriComponentsBuilder.fromUriString("https://www.aiq.ai.kr/login-success")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
