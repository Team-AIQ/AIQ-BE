package cmc.aiq.aiq.global.security.oauth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OAuthOriginCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        if (uri.contains("/oauth2/authorization/")) {
            String origin = request.getParameter("origin");
            String redirectUri = request.getParameter("app_redirect");
            System.out.println("[OAuthOriginFilter] uri=" + uri + " query=" + query);
            System.out.println("[OAuthOriginFilter] origin=" + origin + " app_redirect=" + redirectUri);
            if (origin != null) {
                // 세션에 origin 저장 (iOS 사설 IP에서 쿠키 전송이 불안정하여 세션 사용)
                request.getSession(true).setAttribute("oauth_origin", origin);
                System.out.println("[OAuthOriginFilter] Stored origin in session: " + origin);
            }
            if (redirectUri != null) {
                request.getSession(true).setAttribute("oauth_redirect_uri", redirectUri);
                System.out.println("[OAuthOriginFilter] Stored redirect_uri in session: " + redirectUri);
            }
        }

        filterChain.doFilter(request, response);
    }
}
