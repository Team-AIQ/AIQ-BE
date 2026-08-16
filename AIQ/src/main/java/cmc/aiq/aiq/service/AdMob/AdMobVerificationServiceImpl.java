package cmc.aiq.aiq.service.AdMob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class AdMobVerificationServiceImpl implements AdMobVerificationService {

    @Value("${admob.ssv.key-server-url:https://www.gstatic.com/admob/reward/certs}")
    private String keyServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱용
    private final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    @Override
    public boolean verify(String rawQueryString, String keyId, String signature) {
        try {
            // 1. &signature= 앞부분까지가 AdMob이 서명한 진짜 원본 데이터!
            int signatureIndex = rawQueryString.indexOf("&signature=");
            if (signatureIndex == -1) {
                log.error("원본 쿼리 스트링에 &signature= 가 없습니다.");
                return false;
            }

            String messageToVerify = rawQueryString.substring(0, signatureIndex);
            byte[] data = messageToVerify.getBytes(StandardCharsets.UTF_8);

            // 2. AdMob 서명은 URL-Safe Base64로 오기 때문에 getUrlDecoder() 사용
            byte[] decodedSignature = Base64.getUrlDecoder().decode(signature);

            // 3. AdMob 서버에서 공개키 가져오기
            PublicKey publicKey = getPublicKey(keyId);
            if (publicKey == null) {
                log.error("해당 keyId에 대한 공개키를 찾을 수 없습니다: {}", keyId);
                return false;
            }

            // 4. Java 표준 라이브러리(ECDSA P-256)로 서명 검증!
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(data);

            return sig.verify(decodedSignature);

        } catch (Exception e) {
            log.error("AdMob 서명 검증 중 예기치 않은 오류 발생", e);
            return false;
        }
    }

    private PublicKey getPublicKey(String keyId) throws Exception {
        // 이미 다운받은 키가 있으면 캐시에서 꺼내 씀
        if (publicKeys.containsKey(keyId)) {
            return publicKeys.get(keyId);
        }

        // 캐시에 없으면 AdMob 서버에서 새로 JSON 다운로드
        log.info("AdMob 공개 키 다운로드 시도... keyId: {}", keyId);
        String keysJson = restTemplate.getForObject(keyServerUrl, String.class);

        // Tink 대신 Jackson ObjectMapper를 이용해 평범하게 파싱!
        JsonNode root = objectMapper.readTree(keysJson);
        JsonNode keysNode = root.get("keys");

        if (keysNode != null && keysNode.isArray()) {
            for (JsonNode node : keysNode) {
                String currentKeyId = node.get("keyId").asText();
                String base64Key = node.get("base64").asText();

                // 추출한 base64 문자열을 자바 PublicKey 객체로 변환
                byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("EC");
                PublicKey pubKey = kf.generatePublic(spec);

                // 다음 사용을 위해 Map에 저장
                publicKeys.put(currentKeyId, pubKey);
            }
        }

        return publicKeys.get(keyId);
    }
}