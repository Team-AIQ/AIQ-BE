package cmc.aiq.aiq.service.Curation;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationSubmitRequestDTO;

import java.util.List;

public interface CurationService {
    CurationResponseDTO initiateCuration(CurationRequestDTO request);
    void saveUserAnswers(CurationSubmitRequestDTO request);
    List<HistoryResponseDTO> getUserHistory(Long userId);
    FinalReportResponse getFinalReportOnly(Long userId, Long queryId);
}
