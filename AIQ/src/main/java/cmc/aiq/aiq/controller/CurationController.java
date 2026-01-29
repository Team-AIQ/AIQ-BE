package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.service.ai.CurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/curation")
@RequiredArgsConstructor
public class CurationController {

    private final CurationService curationService;

    @PostMapping("/start")
    public ResponseEntity<CurationResponseDTO> startCuration(@RequestBody CurationRequestDTO request) {
        return ResponseEntity.ok(curationService.initiateCuration(request));
    }
}
