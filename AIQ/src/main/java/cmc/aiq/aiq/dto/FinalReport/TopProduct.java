package cmc.aiq.aiq.dto.FinalReport;

import dev.langchain4j.model.output.structured.Description;
import lombok.Builder; // ★ 추가됨

import java.util.Map;

@Builder // ★ 추가됨: 이제 TopProduct.builder()를 사용할 수 있습니다!
public record TopProduct(@Description("순위 (1~3)")
                         int rank,

                         @Description("제품명")
                         String productName,

                         @Description("제품의 고유 모델명 또는 제품 코드 (예: 'MK183KH/A')")
                         String productCode,

                         @Description("제품의 실제 가격")
                         String price,

                         @Description("제품 이미지 URL")
                         String productImage,


                         @Description("제품 상세 스펙 (항목: 값)")
                         Map<String, String> specs, // Key-Value 형태로 세분화

                         @Description("실제 최저가 구매 링크")
                         String lowestPriceLink,

                         @Description("순위 선정 이유 및 상위 모델과의 차이점 분석 (예: 2등은 1등보다 ~가 부족함)")
                         String comparativeAnalysis) {
}