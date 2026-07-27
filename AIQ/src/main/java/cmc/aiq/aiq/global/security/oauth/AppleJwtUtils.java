package cmc.aiq.aiq.global.security.oauth;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AppleJwtUtils {

    @Value("${apple.team-id}")
    private String APPLE_TEAM_ID;

    @Value("${apple.client-id}")
    private String APPLE_CLIENT_ID;

    @Value("${apple.key-id}")
    private String APPLE_KEY_ID;

    @Value("${apple.private-key}")
    private String APPLE_PRIVATE_KEY;

    /**
     * Apple 서버에 인증하기 위한 client_secret (JWT)을 생성합니다.
     * 이 JWT는 최대 6개월까지 유효합니다.
     */
    public String createClientSecret() {
        Date expirationDate = Date.from(LocalDateTime.now().plusDays(30).atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, APPLE_KEY_ID)
                .setIssuer(APPLE_TEAM_ID)
                .setAudience("https://appleid.apple.com")
                .setSubject(APPLE_CLIENT_ID)
                .setExpiration(expirationDate)
                .setIssuedAt(new Date())
                .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    private PrivateKey getPrivateKey() {
        try {
            String privateKeyString = APPLE_PRIVATE_KEY
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Apple private key를 가져오는 중 오류 발생", e);
        }
    }
}
