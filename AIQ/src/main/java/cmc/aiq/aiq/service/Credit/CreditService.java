package cmc.aiq.aiq.service.Credit;

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
}
