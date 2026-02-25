package cmc.aiq.aiq.global.security;

import cmc.aiq.aiq.global.security.jwt.JwtAuthenticationFilter;
import cmc.aiq.aiq.global.security.jwt.JwtTokenProvider;
import cmc.aiq.aiq.global.security.oauth.CustomOAuth2UserService;
import cmc.aiq.aiq.global.security.oauth.OAuth2SuccessHandler;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // 추가
import org.springframework.web.cors.CorsConfigurationSource; // 추가
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // 중복 코드는 하나로 합치는 게 깔끔합니다.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증이 필요한 API들을 명시적으로 선언
                        .requestMatchers(HttpMethod.POST, "/api/credits/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/withdraw").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/auth/password/change").authenticated()

                        // Preflight 요청(OPTIONS 메서드)은 인증 없이 모두 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/curation/history").hasRole("USER")
                        .requestMatchers("/api/v1/curation/**", "/api/v1/aiq/**")
                        .hasAnyRole("USER", "GUEST")
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 필터 등록
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .exceptionHandling(exception -> exception
                        // 인증되지 않은 사용자가 API 호출 시 302 리다이렉트 대신 401 에러 반환
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.");
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Unauthorized\"}");
                        })
                );


        http.securityContext(context -> context
                .requireExplicitSave(false)
        );
        return http.build();

    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소 허용 (로컬 개발용)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        // 모든 메서드 허용 (GET, POST, PUT, DELETE, OPTIONS 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 모든 헤더 허용 (Authorization 포함)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 쿠키나 인증 헤더를 포함한 요청 허용
        configuration.setAllowCredentials(true);

        // 노출할 헤더 설정 (필요 시)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Authorization-Refresh"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
