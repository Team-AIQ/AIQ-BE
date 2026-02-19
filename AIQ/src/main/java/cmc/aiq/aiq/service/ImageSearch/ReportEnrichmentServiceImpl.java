package cmc.aiq.aiq.service.ImageSearch;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.FinalReport.TopProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportEnrichmentServiceImpl implements ReportEnrichmentService{

    private final GoogleImageSearchService imageSearchService;

    public CompletableFuture<FinalReportResponse> enrichReportWithImages(FinalReportResponse report) {

        // TOP 3 제품 각각에 대해 비동기 검색 수행
        List<CompletableFuture<TopProduct>> productFutures = report.topProducts().stream()
                .map(product -> CompletableFuture.supplyAsync(() -> {
                    String realImageUrl = imageSearchService.getProductImageUrl(product.productName());
                    // Record는 불변이므로 새로운 객체 생성 (이미지 URL 교체)
                    return new TopProduct(
                            product.rank(),
                            product.productName(),
                            product.price(),
                            realImageUrl, // 검색된 실제 URL
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
