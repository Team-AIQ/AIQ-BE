package cmc.aiq.aiq.global.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // /api/auth로 시작하는 모든 경로는 이 필터를 타지 않습니다.
        return path.startsWith("/api/auth") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/login") ||      // [추가]
                path.startsWith("/oauth2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 요청 헤더에서 토큰을 추출합니다.
            String token = resolveToken(request);

            // 2. 토큰이 존재할 때만 검증 로직 실행
            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token)) {
                    // 유효한 경우 인증 객체 저장
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // 토큰은 있지만 유효하지 않은 경우 (이 부분이 누락되어 있었습니다)
                    // 필요에 따라 여기서 커스텀 처리를 하거나, 그냥 다음 필터로 넘겨EntryPoint에서 처리하게 할 수 있습니다.
                }
            }
        } catch (ExpiredJwtException e) {
            // ⭐ 핵심: 토큰이 만료되었을 때 401 에러를 직접 응답합니다.
            log.error("Token Expired: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Access Token이 만료되었습니다.");
            return; // 다음 필터로 진행하지 않고 여기서 종료!
        } catch (JwtException | IllegalArgumentException e) {
            // 기타 토큰 에러 (변조 등)
            log.error("Invalid Token: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
            return;
        }

        // 3. 정상적인 경우 다음 필터로 요청을 넘깁니다.
        filterChain.doFilter(request, response);
    }
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        // JSON 형태로 에러 메시지 작성
        String json = String.format("{\"status\": %d, \"error\": \"Unauthorized\", \"message\": \"%s\"}", status, message);
        response.getWriter().write(json);
    }

    // Header에서 'Bearer '를 제외한 토큰값만 추출하는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
