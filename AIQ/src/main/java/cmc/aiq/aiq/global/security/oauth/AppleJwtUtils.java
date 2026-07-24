package cmc.aiq.aiq.global.security.oauth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleJwtUtils {

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${apple.key-id}")
    private String keyId;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    @Value("${apple.private-key}")
    private String privateKeyString;

    /**
     * Apple OAuth2 통신에 사용할 Client Secret (JWT) 생성
     */
    public String createAppleClientSecret() {
        Date now = new Date();
        // 만료 시간은 보통 한 달(30일) 정도로 설정해 두는 편이야. (애플 정책상 최대 6개월까지 가능해)
        Date expiration = new Date(now.getTime() + (30L * 24 * 60 * 60 * 1000));

        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setHeaderParam("alg", "ES256")
                .setIssuer(teamId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .setAudience("https://appleid.apple.com")
                .setSubject(clientId)
                .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    /**
     * 환경 변수로 주입받은 p8 비대칭 키 문자열을 Java PrivateKey 객체로 변환
     */
    private PrivateKey getPrivateKey() {
        try {
            // Git Secret에서 주입될 때 포함될 수 있는 헤더, 푸터, 줄바꿈을 깔끔하게 제거해 줘
            String privateKeyContent = privateKeyString
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] encodedKey = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);

            // 애플은 Elliptic Curve (EC) 알고리즘을 사용하므로 KeyFactory를 EC로 지정해 줘야 해
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Apple Login용 Private Key 생성에 실패했어.", e);
        }
    }
}