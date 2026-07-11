// OAuth2OriginFilter.java
package cmc.aiq.aiq.global.security.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Log4j2
public class OAuth2OriginFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 소셜 로그인 시작점을 찌를 때만 작동
        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String origin = request.getParameter("origin");
            log.info("🚀 OAuth2 로그인 요청 감지됨! origin 파라미터: {}", origin);

            // 세션 대신 무상태(Stateless)에 적합한 쿠키에 저장
            Cookie cookie = new Cookie("login_origin", "app".equals(origin) ? "app" : "web");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(60 * 5); // 5분간 유효
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}