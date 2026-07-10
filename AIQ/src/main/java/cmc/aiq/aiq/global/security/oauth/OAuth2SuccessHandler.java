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
        
        // [수정] Authentication 객체에서 provider(registrationId)를 직접 가져옵니다.
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String email = extractEmail(attributes, registrationId);

        // [수정] 이제 (email, provider) 조합으로 정확한 사용자를 조회합니다.
        Users user = usersRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new RuntimeException("인증 후 DB에서 회원 정보를 찾을 수 없습니다. email: " + email + ", provider: " + provider));

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
        // [수정] 리다이렉트 URL 분기 처리
        String origin = "web";

        // 1) 기존 파라미터 확인
        String stateParam = request.getParameter("state");

        // 2) 만약 파라미터에 없다면 스프링 시큐리티가 OAuth2 세션에 저장해둔 state 값을 찾습니다.
        if (stateParam == null && request.getSession() != null) {
            // 소셜 로그인 진입 시 임시 저장된 OAuth2 Authorization Request 정보 획득
            Object authorizationRequest = request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (authorizationRequest == null) {
                // 일반적인 OAuth2 로그인 요청 저장소 세션 속성 키 확인
                authorizationRequest = request.getSession().getAttribute("org.springframework.security.web.savedrequest.DefaultSavedRequest");
            }

            // 세션이나 쿼리 문자열에 origin=app이 녹아있는지 포괄적으로 검증
            String queryString = request.getQueryString();
            if ((queryString != null && queryString.contains("origin=app")) ||
                    (stateParam != null && stateParam.contains("origin=app"))) {
                origin = "app";
            }
        } else if (stateParam != null && stateParam.contains("origin=app")) {
            origin = "app";
        }

        // 🌟 혹시 모르니 URI 자체에 state 파라미터 흔적이 있었는지도 더블 체크하는 가장 확실한 방법
        if (request.getRequestURI().contains("origin=app") ||
                (request.getQueryString() != null && request.getQueryString().contains("origin=app"))) {
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

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("email");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("email");
            case "kakao" -> (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}
