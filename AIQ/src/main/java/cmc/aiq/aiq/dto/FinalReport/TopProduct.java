package cmc.aiq.aiq.dto.FinalReport;

import dev.langchain4j.model.output.structured.Description;

public record TopProduct(int rank,
                         String productName,
                         String specs,
                         String lowestPriceLink,
                         @Description("순위 선정 이유 및 상위 모델과의 차이점 분석 (예: 2등은 1등보다 ~가 부족함)")
                         String comparativeAnalysis) {
}
