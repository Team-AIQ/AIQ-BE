package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.service.ai.CurationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "큐레이션 시작", description = "사용자 질문을 분석하여 맞춤형 질문 리스트를 반환합니다.")
    public ResponseEntity<ApiResponse<CurationResponseDTO>> startCuration(@RequestBody CurationRequestDTO request) {
        CurationResponseDTO data = curationService.initiateCuration(request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "큐레이션이 성공적으로 시작되었습니다.", data)
        );
    }
}
