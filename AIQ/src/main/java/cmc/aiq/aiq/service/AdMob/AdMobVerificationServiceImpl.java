package cmc.aiq.aiq.service.AdMob;

import com.google.crypto.tink.JsonKeysetReader; // 수정된 import
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.signature.SignatureConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException; // 추가된 import
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class AdMobVerificationServiceImpl implements AdMobVerificationService {

    @Value("${admob.ssv.key-server-url}")
    private String keyServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, PublicKeyVerify> verifiers = new ConcurrentHashMap<>();

    static {
        try {
            SignatureConfig.register();
        } catch (GeneralSecurityException e) {
            log.error("Tink SignatureConfig 등록 실패", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verify(String customData, String keyId, String signature, String timestamp, String transactionId, String rewardAmount, String rewardItem) {
        try {
            PublicKeyVerify verifier = getVerifier(keyId);
            byte[] data = buildMessage(customData, timestamp, transactionId, rewardAmount, rewardItem);
            byte[] decodedSignature = Base64.getUrlDecoder().decode(signature);

            verifier.verify(decodedSignature, data);
            return true; // 서명 검증 성공
        } catch (GeneralSecurityException | IOException e) { // IOException 추가
            log.warn("AdMob 서명 검증 중 오류 발생. keyId: {}, signature: {}", keyId, signature, e);
            // 캐시된 키가 만료되었을 수 있으므로 캐시를 비우고 다시 시도
            verifiers.remove(keyId);
            try {
                PublicKeyVerify verifier = getVerifier(keyId);
                byte[] data = buildMessage(customData, timestamp, transactionId, rewardAmount, rewardItem);
                byte[] decodedSignature = Base64.getUrlDecoder().decode(signature);
                verifier.verify(decodedSignature, data);
                return true;
            } catch (GeneralSecurityException | IOException ex) { // IOException 추가
                log.error("재시도에도 서명 검증 실패", ex);
                return false;
            }
        } catch (Exception e) {
            log.error("AdMob 서명 검증 중 알 수 없는 오류", e);
            return false;
        }
    }

    private PublicKeyVerify getVerifier(String keyId) throws GeneralSecurityException, IOException { // IOException 추가
        // 캐시된 키가 있으면 사용
        if (verifiers.containsKey(keyId)) {
            return verifiers.get(keyId);
        }

        // 캐시에 없으면 AdMob 서버에서 공개 키 다운로드
        log.info("AdMob 공개 키 다운로드 시도. keyId: {}", keyId);
        String keysJson = restTemplate.getForObject(keyServerUrl, String.class);

        // TinkJsonProtoKeysetReader 대신 JsonKeysetReader 사용
        KeysetHandle keysetHandle = KeysetHandle.readNoSecret(
                JsonKeysetReader.withString(keysJson)
        );

        PublicKeyVerify verifier = keysetHandle.getPrimitive(PublicKeyVerify.class);
        verifiers.put(keyId, verifier); // 다음 사용을 위해 캐시
        return verifier;
    }

    private byte[] buildMessage(String customData, String timestamp, String transactionId, String rewardAmount, String rewardItem) {
        // AdMob 문서에 명시된 대로, 쿼리 파라미터를 사용하여 서명할 원본 메시지 구성
        // 주의: 파라미터 순서가 중요할 수 있으므로, 문서와 동일하게 구성
        return UriComponentsBuilder.newInstance()
                .queryParam("custom_data", customData)
                .queryParam("timestamp", timestamp)
                .queryParam("transaction_id", transactionId)
                .queryParam("reward_amount", rewardAmount)
                .queryParam("reward_item", rewardItem)
                .build()
                .getQuery()
                .getBytes(StandardCharsets.UTF_8);
    }
}
