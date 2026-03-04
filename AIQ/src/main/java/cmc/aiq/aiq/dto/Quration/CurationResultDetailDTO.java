package cmc.aiq.aiq.dto.Quration;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.MultiAiDTO.AiRecommendationResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurationResultDetailDTO {
    private FinalReportResponse finalReport;
    private List<AiRecommendationResponse> individualReports;
}
