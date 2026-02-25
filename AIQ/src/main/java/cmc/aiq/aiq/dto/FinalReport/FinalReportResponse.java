package cmc.aiq.aiq.dto.FinalReport;

import dev.langchain4j.model.output.structured.Description; // Description 어노테이션 import

import java.util.List;

public record FinalReportResponse(
        @Description("모든 AI 모델들의 공통적인 의견을 요약한 내용입니다. 사용자의 상황에 대한 공감을 포함해야 합니다.")
        String consensus,

        @Description("모델별로 의견이 갈린 지점을 '성능 vs 가격'과 같은 현실적인 선택의 기로로 묘사한 내용입니다.")
        String decisionBranches,

        @Description("AIQ가 TOP 3 제품을 종합적으로 분석하여, 왜 이 조합을 추천하는지에 대한 핵심적인 이유입니다.")
        String aiqRecommendationReason, // <-- 새로운 필드 추가

        @Description("최종 추천 TOP 3 제품 리스트입니다.")
        List<TopProduct> topProducts,

        @Description("사용자가 후회 없는 결정을 내릴 수 있도록 확신을 주는 마지막 한마디입니다.")
        String finalWord
) {
}
