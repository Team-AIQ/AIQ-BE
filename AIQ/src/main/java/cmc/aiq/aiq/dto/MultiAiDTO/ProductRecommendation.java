package cmc.aiq.aiq.dto.MultiAiDTO;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record ProductRecommendation(@Description("추천하는 제품의 정확한 전체 이름 (예: 'Apple MacBook Pro 16-inch')")
                                    String productName,

                                    @Description("제품의 고유 모델명 또는 제품 코드 (예: 'MK183KH/A')")
                                    String productCode,

                                    @Description("해당 모델 추천 대상")
                                    String targetAudience,

                                    @Description("선정 이유 4가지 (정확히 4개를 작성해야 함)")
                                    List<String> selectionReasons) {
}
