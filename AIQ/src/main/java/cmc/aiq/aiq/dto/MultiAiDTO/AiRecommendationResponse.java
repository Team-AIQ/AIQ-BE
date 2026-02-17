package cmc.aiq.aiq.dto.MultiAiDTO;


import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record AiRecommendationResponse(
        @Description("응답을 제공한 AI 모델명 (예: GPT, Gemini  등)")
        String modelName,
        @Description("3개의 추천 모델 리스트")
        List<ProductRecommendation> recommendations,

        @Description("해당 카테고리 제품 구매 시 고려해야 할 상세 스펙 가이드")
        String specGuide,

        @Description("실패 확률을 0%로 만드는 마지막 한마디")
        String finalWord
) {}

