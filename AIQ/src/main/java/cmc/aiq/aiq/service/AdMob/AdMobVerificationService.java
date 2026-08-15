package cmc.aiq.aiq.service.AdMob;

public interface AdMobVerificationService {
    /**
     * AdMob 보상형 광고 콜백의 서명을 검증합니다.
     *
     * @param rawQueryString 디코딩되지 않은 원본 쿼리 스트링 전체 (signature 앞부분까지)
     * @param keyId          서명에 사용된 공개 키의 ID
     * @param signature      Base64 인코딩된 서명 값
     * @return 서명이 유효하면 true, 그렇지 않으면 false
     */
    boolean verify(String rawQueryString, String keyId, String signature);
}
