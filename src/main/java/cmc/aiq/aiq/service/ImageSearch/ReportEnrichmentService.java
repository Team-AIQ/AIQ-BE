package cmc.aiq.aiq.service.ImageSearch;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;

import java.util.concurrent.CompletableFuture;

public interface ReportEnrichmentService {
    CompletableFuture<FinalReportResponse> enrichReportWithImages(FinalReportResponse report);
}
