package cmc.aiq.aiq.dto.FinalReport;

import dev.langchain4j.model.output.structured.Description;

import java.util.Map;

public record TopProduct(@Description("순위 (1~3)")
                         int rank,

                         @Description("제품명")
                         String productName,

                         @Description("제품 이미지 URL")
                         String productImage,

                         @Description("제품 상세 스펙 (항목: 값)")
                         Map<String, String> specs, // Key-Value 형태로 세분화

                         @Description("실제 최저가 구매 링크")
                         String lowestPriceLink,
                         @Description("순위 선정 이유 및 상위 모델과의 차이점 분석 (예: 2등은 1등보다 ~가 부족함)")
                         String comparativeAnalysis) {
}
