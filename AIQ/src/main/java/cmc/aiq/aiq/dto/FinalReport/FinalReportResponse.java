package cmc.aiq.aiq.dto.FinalReport;

import org.springframework.context.annotation.Description;

import java.util.List;

public record FinalReportResponse(@Description("모든 AI 모델들의 공통적인 의견 요약")
                                  String consensus,

                                  @Description("모델별로 판단이 갈리는 기준점과 이유 분석")
                                  String decisionBranches,

                                  @Description("최종 추천 TOP 3 제품 리스트")
                                  List<TopProduct> topProducts,

                                  @Description("실패 확률을 0%로 만드는 마지막 한마디")
                                  String finalWord) {
}
