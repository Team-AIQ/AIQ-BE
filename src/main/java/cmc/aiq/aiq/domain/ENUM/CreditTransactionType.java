package cmc.aiq.aiq.domain.ENUM;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CreditTransactionType {

    REPORT_GENERATION(3L, "큐레이션 리포트 생성"),
    RECOMMEND_TOP3(10L, "TOP3 추천");

    private final Long cost;
    private final String description;
}
