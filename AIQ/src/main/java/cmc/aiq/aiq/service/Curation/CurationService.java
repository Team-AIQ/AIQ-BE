package cmc.aiq.aiq.service.Curation;

import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationSubmitRequestDTO;

public interface CurationService {
    CurationResponseDTO initiateCuration(CurationRequestDTO request);
    void saveUserAnswers(CurationSubmitRequestDTO request);
}
