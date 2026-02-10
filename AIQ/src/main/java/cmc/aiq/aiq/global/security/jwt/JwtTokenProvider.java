package cmc.aiq.aiq.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Log4j2
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.remember-me-expiration}")
    private long rememberMeExpiration;

    private Key key;

    @PostConstruct
    protected void init() {
        // 설정된 secretKey를 바탕으로 HMAC SHA 암호화 키 생성
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String email, String role , String nickname) {
        return createToken(userId, email, accessExpiration , false , role, nickname);
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId, String email , String role, boolean isRememberMe) {
        long validity = isRememberMe ? rememberMeExpiration : refreshExpiration;
        return createToken(userId, email, validity , isRememberMe , role , null);
    }

    // 1. 토큰 생성 (이메일과 ID를 담아 암호화)
    private String createToken(Long userId, String email, long validity , boolean isRememberMe, String role , String nickname) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("userId", userId);
        claims.put("auth", role);
        claims.put("isRememberMe", isRememberMe);
        if (nickname != null) {
            claims.put("nickname", nickname);
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. 토큰에서 사용자 정보(Email) 추출
    public String getUserEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // 3. 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        log.info("====== JWT CLAIMS DEBUG ======");
        log.info("전체 클레임: " + claims.toString());
        log.info("auth 값: " + claims.get("auth"));
        log.info("==============================");
        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }


        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(role -> {
                            // 이미 ROLE_로 시작하면 그대로 쓰고, 아니면 붙여줍니다.
                            String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                            return new SimpleGrantedAuthority(roleName);
                        })
                        .collect(Collectors.toList());

        // Spring Security의 User 객체 생성 (비밀번호는 보안상 빈 값)
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
    public boolean getIsRememberMe(String token) {
        return parseClaims(token).get("isRememberMe", Boolean.class);
    }
}
