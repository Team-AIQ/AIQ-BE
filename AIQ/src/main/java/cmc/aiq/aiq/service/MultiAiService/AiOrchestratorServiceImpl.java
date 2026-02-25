package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.CurationSessions;
import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.domain.Models;
import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.MultiAiDTO.AiRecommendationResponse;
import cmc.aiq.aiq.dto.MultiAiDTO.ProductRecommendation;
import cmc.aiq.aiq.repository.AiResponseRepository;
import cmc.aiq.aiq.repository.CurationSessionsRepository;
import cmc.aiq.aiq.repository.ModelsRepository;
import cmc.aiq.aiq.repository.QueriesRepository;
import cmc.aiq.aiq.service.Credit.CreditService;
import cmc.aiq.aiq.service.ImageSearch.ReportEnrichmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiOrchestratorServiceImpl implements AiOrchestratorService {
    private final ChatLanguageModel gptModel;
    private final ChatLanguageModel geminiModel;
    private final ChatLanguageModel perplexityModel;
    private final ObjectMapper objectMapper;
    private final ReportAgent reportAgent;

    private final PromptManager promptManager;
    private final AiResponseRepository aiResponseRepository;
    private final QueriesRepository queriesRepository;
    private final CurationSessionsRepository curationSessionsRepository;
    private final ModelsRepository modelsRepository;

    private final CurationTextBuilder curationTextBuilder;
    private final DelegatingSecurityContextAsyncTaskExecutor taskExecutor;
    private final ReportEnrichmentService reportEnrichmentService;
    private final CreditService creditService;

    @Override
    @Transactional
    public void executeParallelAi(Long queryId, List<String> selectedModels, SseEmitter emitter) {
        SecurityContext mainContext = SecurityContextHolder.getContext();
        
        // [삭제] 크레딧 차감 로직 및 관련 try-catch 제거
        Queries queries = queriesRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("질문 정보를 찾을 수 없습니다."));
        String userQuestion = queries.getQuestion();

        CurationSessions session = curationSessionsRepository.findByQueryId(queryId)
                .orElseThrow(() -> new RuntimeException("큐레이션 세션을 찾을 수 없습니다."));

        String curationContext = curationTextBuilder.build(session);
        String categoryName = session.getCategoryAttributes().getDisplayName();

        Map<String, String> variables = Map.of(
                "categoryName", categoryName,
                "context", curationContext,
                "question", userQuestion
        );

        String systemPrompt = promptManager.getProcessedPrompt("AI_RECOMMEND_SYSTEM", variables);

        List<CompletableFuture<AiRecommendationResponse>> futures = new ArrayList<>();
        List<String> targets = selectedModels.stream().map(String::trim).collect(Collectors.toList());

        if (targets.contains("GPT")) {
            futures.add(callAi(gptModel, "GPT", systemPrompt, userQuestion, queries, emitter, mainContext));
        }
        if (targets.contains("Gemini")) {
            futures.add(callAi(geminiModel, "Gemini", systemPrompt, userQuestion, queries, emitter, mainContext));
        }
        if (targets.contains("Perplexity")) {
            futures.add(callAi(perplexityModel, "Perplexity", systemPrompt, userQuestion, queries, emitter, mainContext));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRunAsync(() -> {
                    SecurityContextHolder.setContext(mainContext);
                    try {
                        List<AiRecommendationResponse> responses = futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList());

                        String combinedText = formatResponsesForReport(responses);
                        log.info("모든 모델 응답 완료. 최종 보고서 생성 시작...");

                        String systemPromptTemplate = promptManager.getProcessedPrompt("REPORT_AGENT_SYSTEM", Map.of());
                        Result<FinalReportResponse> reportResult = reportAgent.generateReport(
                                systemPromptTemplate,
                                userQuestion,
                                curationContext,
                                combinedText,
                                categoryName
                        );

                        FinalReportResponse rawReport = reportResult.content();

                        log.info("제품 이미지 검색 시작...");
                        FinalReportResponse enrichedReport = reportEnrichmentService
                                .enrichReportWithImages(rawReport)
                                .join();

                        long reportStartTime = System.currentTimeMillis();
                        AiResponse reportRecord = saveInitialPending(queries, "GPT", ResponseType.FINAL_REPORT);
                        saveCompletion(reportRecord.getId(), reportResult, enrichedReport, reportStartTime);

                        sendSse(emitter, "FINAL_REPORT", enrichedReport);
                        sendSse(emitter, "finish", "done");
                        emitter.complete();

                    } catch (Exception e) {
                        log.error("최종 보고서 생성 중 에러", e);
                        sendSse(emitter, "ERROR", e.getMessage());
                        emitter.completeWithError(e);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }, taskExecutor);
    }

    @Override
    @Transactional
    public CompletableFuture<AiRecommendationResponse> callAi(ChatLanguageModel model, String modelName, String systemPrompt,
                                                              String question, Queries queries, SseEmitter emitter, SecurityContext context) {
        AiResponse record = saveInitialPending(queries, modelName, ResponseType.INDIVIDUAL);
        final Long responseId = record.getId();

        return CompletableFuture.supplyAsync(() -> {
            SecurityContextHolder.setContext(context);
            long startTime = System.currentTimeMillis();
            try {
                RecommendationAgent agent = AiServices.create(RecommendationAgent.class, model);
                Result<AiRecommendationResponse> result = agent.generate(systemPrompt, question);
                AiRecommendationResponse aiOutput = result.content();

                AiRecommendationResponse finalResponse = new AiRecommendationResponse(
                        modelName,
                        aiOutput.recommendations(),
                        aiOutput.specGuide(),
                        aiOutput.finalWord()
                );

                sendSse(emitter, modelName + "_ANSWER", finalResponse);
                saveCompletion(responseId, result, finalResponse, startTime);

                return result.content();
            } catch (Exception e) {
                log.error("{} 호출 에러: {}", modelName, e.getMessage());
                updateToFailed(responseId, e.getMessage());
                return null;
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, taskExecutor);
    }

    @Override
    public void updateToFailed(Long recordId, String error) {
        aiResponseRepository.findById(recordId).ifPresent(r -> {
            r.fail(error);
            aiResponseRepository.saveAndFlush(r);
        });
    }

    @Override
    public AiResponse saveInitialPending(Queries queries, String modelName, ResponseType type) {
        Models model = modelsRepository.findByName(modelName)
                .orElseThrow(() -> new RuntimeException("모델 정보를 찾을 수 없습니다: " + modelName));
        AiResponse response = AiResponse.builder()
                .queries(queries)
                .model(model)
                .responseType(type)
                .build();
        return aiResponseRepository.save(response);
    }

    @Override
    @Transactional
    public <T> void saveCompletion(Long recordId, Result<T> result, T content, long startTime) {
        try {
            AiResponse record = aiResponseRepository.findByIdWithModel(recordId)
                    .orElseThrow(() -> new RuntimeException("저장할 레코드를 찾을 수 없습니다."));

            long latency = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("latency_ms", latency);

            if (result.tokenUsage() != null) {
                metadata.put("token_usage", Map.of(
                        "input_tokens", result.tokenUsage().inputTokenCount(),
                        "output_tokens", result.tokenUsage().outputTokenCount(),
                        "total_tokens", result.tokenUsage().totalTokenCount()
                ));
            }

            String jsonContent = objectMapper.writeValueAsString(content);
            record.complete(jsonContent, metadata);
            aiResponseRepository.saveAndFlush(record);
            log.info("DB 저장 성공 - 모델: {}, 지연시간: {}ms", record.getModel().getName(), latency);
        } catch (JsonProcessingException e) {
            log.error("응답 저장 중 오류 발생 (ID: {}): {}", recordId, e.getMessage());
        }
    }

    private void sendSse(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.warn("SSE 전송 실패 (클라이언트가 연결을 끊었을 수 있음): {}", e.getMessage());
        }
    }

    private String formatResponsesForReport(List<AiRecommendationResponse> responses) {
        StringBuilder sb = new StringBuilder();
        for (AiRecommendationResponse response : responses) {
            if (response != null) {
                appendModelOutput(sb, response.modelName(), response);
            }
        }
        return sb.toString();
    }

    private void appendModelOutput(StringBuilder sb, String modelName, AiRecommendationResponse response) {
        sb.append("[").append(modelName).append(" 추천 제품]\n");
        for (ProductRecommendation rec : response.recommendations()) {
            sb.append("- 모델명: ").append(rec.productName()).append("\n");
            sb.append("- 제품 코드: ").append(rec.productCode()).append("\n");
            sb.append("  추천대상: ").append(rec.targetAudience()).append("\n");
            sb.append("  선정이유: ").append(String.join(", ", rec.selectionReasons())).append("\n");
        }
        sb.append("스펙가이드: ").append(response.specGuide()).append("\n\n");
    }
}
