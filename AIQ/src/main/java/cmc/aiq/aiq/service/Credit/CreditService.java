package cmc.aiq.aiq.service.Credit;

import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;

import java.math.BigDecimal;

public interface CreditService {
    /**
     * 특정 사용자에게 크레딧을 지급하고 로그를 남깁니다.
     *
     * @param userId 사용자 ID
     * @param amount 지급할 크레딧 양
     * @param reason 지급 사유 (예: "보상형 광고 시청")
     */
    void grantCredit(Long userId, BigDecimal amount, String reason);

    /**
     * 특정 사용자의 크레딧을 사용합니다.
     * 게스트 유저는 크레딧을 차감하지 않습니다.
     *
     * @param userId 사용자 ID
     * @param type   사용할 크레딧의 종류와 양 (ENUM)
     */
    void useCredit(Long userId, CreditTransactionType type);
}
