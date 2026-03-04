package cmc.aiq.aiq.service.Curation;

import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationResultDetailDTO;
import cmc.aiq.aiq.dto.Quration.CurationSubmitRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface CurationService {
    CurationResponseDTO initiateCuration(CurationRequestDTO request);
    void saveUserAnswers(CurationSubmitRequestDTO request) throws JsonProcessingException;
    List<HistoryResponseDTO> getUserHistory(Long userId);
    CurationResultDetailDTO getCurationResultDetail(Long userId, Long queryId);
}
