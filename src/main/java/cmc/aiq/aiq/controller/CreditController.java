package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.global.security.CustomUserDetails;
import cmc.aiq.aiq.service.Credit.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "크레딧", description = "사용자 크레딧 사용/조회 관련 API")
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Log4j2
public class CreditController {

    private final CreditService creditService;

    @PostMapping("/use/top3")
    @Operation(summary = "TOP3 추천 기능 사용", description = "TOP3 추천 기능 사용 시 10 크레딧을 소모합니다.")
    public ResponseEntity<ApiResponse<Void>> useTop3RecommendationCredit(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            creditService.useCredit(userDetails.getUserId(), CreditTransactionType.RECOMMEND_TOP3);
            log.info("TOP3 추천 크레딧 사용 성공: userId={}", userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "크레딧이 성공적으로 사용되었습니다.", null));
        } catch (IllegalStateException e) {
            log.warn("크레딧 부족으로 TOP3 추천 기능 사용 실패: userId={}", userDetails.getUserId(), e);
            // [수정] .name()을 제거하여 HttpStatus 객체를 직접 전달합니다.
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.failure(HttpStatus.PAYMENT_REQUIRED, e.getMessage()));
        } catch (Exception e) {
            log.error("크레딧 사용 중 알 수 없는 오류 발생: userId={}", userDetails.getUserId(), e);
            // [수정] .name()을 제거하여 HttpStatus 객체를 직접 전달합니다.
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
        }
    }
}