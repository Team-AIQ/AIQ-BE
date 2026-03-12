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
public class ReportEnrichmentServiceImpl implements ReportEnrichmentService {

    private final GoogleImageSearchService imageSearchService;
    private static final String PLACEHOLDER_IMAGE = "https://placehold.co/600x400?text=No+Image";

    public CompletableFuture<FinalReportResponse> enrichReportWithImages(FinalReportResponse report) {

        List<CompletableFuture<TopProduct>> productFutures = report.topProducts().stream()
                .map(product -> CompletableFuture.supplyAsync(() -> {
                    String imageUrl = PLACEHOLDER_IMAGE;
                    String productName = product.productName();
                    String productCode = product.productCode();

                    if (isProductCodeValid(productName, productCode)) {
                        imageUrl = imageSearchService.getProductImageUrl(productCode);
                        log.info("제품 코드로 이미지 검색 시도: {} -> 결과: {}", productCode, imageUrl);
                    }

                    if (PLACEHOLDER_IMAGE.equals(imageUrl) && StringUtils.hasText(productName)) {
                        imageUrl = imageSearchService.getProductImageUrl(productName);
                        log.info("제품명으로 이미지 재검색 시도: {} -> 결과: {}", productName, imageUrl);
                    }

                    return new TopProduct(
                            product.rank(),
                            productName,
                            productCode,
                            product.price(),
                            imageUrl,
                            product.specs(),
                            product.lowestPriceLink(),
                            product.comparativeAnalysis()
                    );
                }))
                .toList();

        return CompletableFuture.allOf(productFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<TopProduct> enrichedProducts = productFutures.stream()
                            .map(CompletableFuture::join)
                            .toList();

                    // [수정] 새로운 생성자 시그니처에 맞게 aiqRecommendationReason 필드를 전달합니다.
                    return new FinalReportResponse(
                            report.consensus(),
                            report.decisionBranches(),
                            report.aiqRecommendationReason(), // <-- 이 부분을 추가했습니다.
                            enrichedProducts,
                            report.finalWord()
                    );
                });
    }

    private boolean isProductCodeValid(String productName, String productCode) {
        if (!StringUtils.hasText(productName) || !StringUtils.hasText(productCode)) {
            return false;
        }
        String simplifiedProductName = productName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String simplifiedProductCode = productCode.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        return simplifiedProductName.contains(simplifiedProductCode);
    }
}
