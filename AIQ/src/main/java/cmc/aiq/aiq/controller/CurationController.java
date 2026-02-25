package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
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
        try {
            CurationResponseDTO data = curationService.initiateCuration(request);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK, "큐레이션이 성공적으로 시작되었습니다.", data)
            );
        } catch (IllegalStateException e) {
            log.warn("크레딧 부족으로 큐레이션 시작 실패: userId={}, message={}", request.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.failure(HttpStatus.PAYMENT_REQUIRED, e.getMessage()));
        } catch (Exception e) {
            log.error("큐레이션 시작 중 알 수 없는 오류 발생: userId={}", request.getUserId(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
        }
    }

    @PostMapping("/submit")
    @Operation(summary = "큐레이션 답변 제출", description = "사용자가 선택한 답변들을 세션에 저장합니다.")
    public ResponseEntity<ApiResponse<Void>> submitAnswers(@RequestBody CurationSubmitRequestDTO request) throws JsonProcessingException {
        log.info("파싱된 답변 데이터: {}", new ObjectMapper().writeValueAsString(request.getAnswers()));
        curationService.saveUserAnswers(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "사용자 답변이 성공적으로 저장되었습니다.", null));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HistoryResponseDTO>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = user.getUserId();
        List<HistoryResponseDTO> history = curationService.getUserHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "히스토리 조회 성공", history));
    }

    @GetMapping("/history/{queryId}/report")
    public ResponseEntity<ApiResponse<FinalReportResponse>> getReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long queryId
    ) {
        FinalReportResponse report = curationService.getFinalReportOnly(user.getUserId(), queryId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "최종 보고서 조회 성공", report));
    }
}
