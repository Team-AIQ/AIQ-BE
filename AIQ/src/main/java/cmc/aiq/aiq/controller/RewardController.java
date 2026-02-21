package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.service.AdMob.AdMobVerificationService;
import cmc.aiq.aiq.service.Credit.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "AdMob 보상", description = "AdMob 보상형 광고 서버 측 검증(SSV) 관련 API")
@RestController
@RequestMapping("/api/v1/reward")
@RequiredArgsConstructor
@Log4j2
public class RewardController {

    private final AdMobVerificationService adMobVerificationService;
    private final CreditService creditService;

    @GetMapping("/admob")
    @Operation(summary = "AdMob 보상형 광고 콜백", description = "AdMob 서버가 광고 시청 완료 시 호출하는 엔드포인트입니다. 직접 호출하지 마세요.")
    public ResponseEntity<Void> handleAdMobReward(
            @RequestParam("custom_data") String customData,
            @RequestParam("key_id") String keyId,
            @RequestParam("signature") String signature,
            @RequestParam("user_id") String googleUserId, // AdMob에서 제공하는 사용자 ID
            @RequestParam("ad_network") String adNetwork,
            @RequestParam("ad_unit") String adUnit,
            @RequestParam("reward_amount") String rewardAmount,
            @RequestParam("reward_item") String rewardItem,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("transaction_id") String transactionId
    ) {
        log.info("AdMob 보상 콜백 수신: userId={}, transactionId={}", customData, transactionId);

        // 1. AdMob 요청 서명 검증
        boolean isVerified = adMobVerificationService.verify(customData, keyId, signature, timestamp, transactionId, rewardAmount, rewardItem);

        if (isVerified) {
            // 2. 서명 검증 성공 시 보상 지급
            try {
                Long userId = Long.parseLong(customData);
                BigDecimal amount = new BigDecimal(rewardAmount);

                creditService.grantCredit(userId, amount, "보상형 광고 시청");
                log.info("보상 지급 완료: userId={}, amount={}", userId, amount);

                // AdMob 서버에게 성공적으로 처리했음을 알림 (HTTP 200 OK)
                return ResponseEntity.ok().build();

            } catch (NumberFormatException e) {
                log.error("custom_data 파싱 오류. 유효한 Long 타입의 userId가 필요합니다. customData: {}", customData, e);
                // 잘못된 요청으로 처리
                return ResponseEntity.badRequest().build();
            } catch (Exception e) {
                log.error("보상 지급 중 서버 내부 오류 발생", e);
                // 서버 내부 오류
                return ResponseEntity.internalServerError().build();
            }
        } else {
            // 3. 서명 검증 실패 시
            log.warn("AdMob 서명 검증 실패. transactionId: {}", transactionId);
            // 위변조된 요청일 수 있으므로 400 Bad Request 또는 403 Forbidden 응답
            return ResponseEntity.badRequest().build();
        }
    }
}
