package cmc.aiq.aiq.dto.MultiAiDTO;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record ProductRecommendation(@Description("추천 모델 명")
                                    String modelName,

                                    @Description("해당 모델 추천 대상")
                                    String targetAudience,

                                    @Description("선정 이유 4가지 (정확히 4개를 작성해야 함)")
                                    List<String> selectionReasons) {
}
