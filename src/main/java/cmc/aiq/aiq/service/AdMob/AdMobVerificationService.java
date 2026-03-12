package cmc.aiq.aiq.service.AdMob;

public interface AdMobVerificationService {
    /**
     * AdMob 보상형 광고 콜백의 서명을 검증합니다.
     *
     * @param customData    광고 요청 시 앱에서 보낸 custom_data
     * @param keyId         서명에 사용된 공개 키의 ID
     * @param signature     Base64 인코딩된 서명
     * @param timestamp     타임스탬프
     * @param transactionId 트랜잭션 ID
     * @param rewardAmount  보상 양
     * @param rewardItem    보상 아이템
     * @return 서명이 유효하면 true, 그렇지 않으면 false
     */
    boolean verify(String customData, String keyId, String signature, String timestamp, String transactionId, String rewardAmount, String rewardItem);
}
