package cmc.aiq.aiq.service.ai;

import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;

public interface CurationService {
    CurationResponseDTO initiateCuration(CurationRequestDTO request);
}
