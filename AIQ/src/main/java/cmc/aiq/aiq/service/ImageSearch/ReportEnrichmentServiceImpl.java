package cmc.aiq.aiq.service.ImageSearch;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.FinalReport.TopProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportEnrichmentServiceImpl implements ReportEnrichmentService{

    private final GoogleImageSearchService imageSearchService;
    private static final String PLACEHOLDER_IMAGE = "https://placehold.co/600x400?text=No+Image";

    public CompletableFuture<FinalReportResponse> enrichReportWithImages(FinalReportResponse report) {

        // TOP 3 제품 각각에 대해 비동기 검색 수행
        List<CompletableFuture<TopProduct>> productFutures = report.topProducts().stream()
                .map(product -> CompletableFuture.supplyAsync(() -> {
                    String imageUrl = PLACEHOLDER_IMAGE;

                    // 1. 제품 코드가 있으면 제품 코드로 먼저 검색
                    if (StringUtils.hasText(product.productCode())) {
                        imageUrl = imageSearchService.getProductImageUrl(product.productCode());
                        log.info("제품 코드로 이미지 검색 시도: {} -> 결과: {}", product.productCode(), imageUrl);
                    }

                    // 2. 제품 코드로 검색 실패 시 (플레이스홀더 반환 시), 제품명으로 다시 검색
                    if (PLACEHOLDER_IMAGE.equals(imageUrl) && StringUtils.hasText(product.productName())) {
                        imageUrl = imageSearchService.getProductImageUrl(product.productName());
                        log.info("제품명으로 이미지 재검색 시도: {} -> 결과: {}", product.productName(), imageUrl);
                    }

                    // Record는 불변이므로 새로운 객체 생성 (이미지 URL 교체)
                    return new TopProduct(
                            product.rank(),
                            product.productName(),
                            product.productCode(), // DTO에 맞게 productCode 추가
                            product.price(),
                            imageUrl, // 최종적으로 결정된 이미지 URL
                            product.specs(),
                            product.lowestPriceLink(),
                            product.comparativeAnalysis()
                    );
                }))
                .toList();

        // 모든 검색이 완료되면 리포트 재구성
        return CompletableFuture.allOf(productFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<TopProduct> enrichedProducts = productFutures.stream()
                            .map(CompletableFuture::join)
                            .toList();

                    return new FinalReportResponse(
                            report.consensus(),
                            report.decisionBranches(),
                            enrichedProducts,
                            report.finalWord()
                    );
                });
    }
}
