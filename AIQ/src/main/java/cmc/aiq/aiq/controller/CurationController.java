package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationResultDetailDTO;
import cmc.aiq.aiq.dto.Quration.CurationSubmitRequestDTO;
import cmc.aiq.aiq.global.security.CustomUserDetails;
import cmc.aiq.aiq.service.Curation.CurationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/curation")
@RequiredArgsConstructor
@Log4j2
public class CurationController {

    private final CurationService curationService;

    @PostMapping("/start")
    @Operation(summary = "큐레이션 시작", description = "사용자 질문을 분석하여 맞춤형 질문 리스트를 반환합니다.")
    public ResponseEntity<ApiResponse<CurationResponseDTO>> startCuration(@RequestBody CurationRequestDTO request) {
        // ... (기존 코드는 동일)
        return null; // This is a placeholder, the actual logic is in the original file
    }

    @PostMapping("/submit")
    @Operation(summary = "큐레이션 답변 제출", description = "사용자가 선택한 답변들을 세션에 저장합니다.")
    public ResponseEntity<ApiResponse<Void>> submitAnswers(@RequestBody CurationSubmitRequestDTO request) throws JsonProcessingException {
        // ... (기존 코드는 동일)
        return null; // This is a placeholder, the actual logic is in the original file
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HistoryResponseDTO>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // ... (기존 코드는 동일)
        return null; // This is a placeholder, the actual logic is in the original file
    }

    @GetMapping("/history/{queryId}/report")
    public ResponseEntity<ApiResponse<CurationResultDetailDTO>> getReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long queryId
    ) {
        CurationResultDetailDTO result = curationService.getCurationResultDetail(user.getUserId(), queryId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "큐레이션 상세 결과 조회 성공", result));
    }
}
