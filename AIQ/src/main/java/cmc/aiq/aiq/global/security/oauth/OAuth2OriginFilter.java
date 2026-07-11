// OAuth2OriginFilter.java
package cmc.aiq.aiq.global.security.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OAuth2OriginFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 소셜 로그인 시작점(/oauth2/authorization/...)을 찌를 때만 작동
        if (request.getRequestURI().startsWith("/oauth2/authorization/")) {
            String origin = request.getParameter("origin");
            if ("app".equals(origin)) {
                // 파라미터가 유실되기 전에 세션에 안전하게 보관!
                request.getSession().setAttribute("login_origin", "app");
            } else {
                request.getSession().setAttribute("login_origin", "web");
            }
        }

        filterChain.doFilter(request, response);
    }
}