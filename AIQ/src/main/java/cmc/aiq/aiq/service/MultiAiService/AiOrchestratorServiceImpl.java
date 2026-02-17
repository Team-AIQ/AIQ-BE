package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.CurationSessions;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Log4j2
public class AiOrchestratorServiceImpl implements AiOrchestratorService {
    private final ChatLanguageModel gptModel;
    private final ChatLanguageModel geminiModel;
    private final ChatLanguageModel perplexityModel;
    private final ObjectMapper objectMapper;
    private final ReportAgent reportAgent; // LangChain4j AiService

    private final PromptManager promptManager;
    private final AiResponseRepository aiResponseRepository;
    private final QueriesRepository queriesRepository;
    private final CurationSessionsRepository curationSessionsRepository;
    private final ModelsRepository modelsRepository;

    private final CurationTextBuilder curationTextBuilder;
    private final DelegatingSecurityContextAsyncTaskExecutor taskExecutor;

    @Override
    @Transactional
    public void executeParallelAi(Long queryId, SseEmitter emitter) {
        SecurityContext mainContext = SecurityContextHolder.getContext();
        // 1. 기초 데이터 로드
        Queries queries = queriesRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("질문 정보를 찾을 수 없습니다."));
        String userQuestion = queries.getQuestion();

        CurationSessions session = curationSessionsRepository.findByQueryId(queryId)
                .orElseThrow(() -> new RuntimeException("큐레이션 세션을 찾을 수 없습니다."));

        // 2. 프롬프트 변수 준비 (기존 유지)
        String curationContext = curationTextBuilder.build(session);
        String categoryName = session.getCategoryAttributes().getDisplayName();

        Map<String, String> variables = Map.of(
                "categoryName", categoryName,
                "context", curationContext,
                "question", queries.getQuestion()
        );

        // 3. 치환된 시스템 프롬프트 가져오기
        String systemPrompt = promptManager.getProcessedPrompt("AI_RECOMMEND_SYSTEM", variables);


        // 5. [Scatter] 비동기 병렬 호출 시작 (반환 타입이 AiRecommendationResponse로 변경됨)
        CompletableFuture<AiRecommendationResponse> gptFuture = callAi(gptModel, "GPT", systemPrompt, userQuestion, queries, emitter, mainContext);
        CompletableFuture<AiRecommendationResponse> geminiFuture = callAi(geminiModel, "Gemini", systemPrompt, userQuestion, queries, emitter, mainContext);
        CompletableFuture<AiRecommendationResponse> perplexityFuture = callAi(perplexityModel, "Perplexity", systemPrompt, userQuestion, queries, emitter,  mainContext);

        // 6. [Gather] 모든 응답 완료 후 최종 보고서 생성
        CompletableFuture.allOf(gptFuture, geminiFuture, perplexityFuture)
                .thenRunAsync(() -> {
                    SecurityContextHolder.setContext(mainContext);
                    try {
                        // 1) 비동기 결과 취합 (join)
                        String combinedText = formatResponsesForReport(gptFuture.join(), geminiFuture.join(), perplexityFuture.join());
                        log.info("모든 모델 응답 완료. 최종 보고서 생성 시작...");

                        String systemPromptTemplate = promptManager.getProcessedPrompt("REPORT_AGENT_SYSTEM", Map.of());
                        long reportStartTime = System.currentTimeMillis();
                        Result<FinalReportResponse> reportResult = reportAgent.generateReport(
                                systemPromptTemplate, // DB에서 꺼낸 그 날카로운 프롬프트!
                                userQuestion,
                                curationContext,
                                combinedText,
                                categoryName
                        );

                        // 4) 저장 및 전송 (우리가 만든 제네릭 saveCompletion 사용)
                        AiResponse reportRecord = saveInitialPending(queries, "GPT", ResponseType.FINAL_REPORT);
                        saveCompletion(reportRecord.getId(), reportResult,reportResult.content(), reportStartTime);

                        sendSse(emitter, "FINAL_REPORT", reportResult.content());
                        sendSse(emitter, "finish", "done");
                        emitter.complete();

                    } catch (Exception e) {
                        log.error("최종 보고서 생성 중 에러", e);
                        sendSse(emitter, "ERROR", e.getMessage());
                        emitter.completeWithError(e);
                    } finally{
                        SecurityContextHolder.clearContext();
                    }
                }, taskExecutor);
    }

    @Override
    @Transactional
    public CompletableFuture<AiRecommendationResponse> callAi(ChatLanguageModel model, String modelName, String systemPrompt,
                                                              String question, Queries queries, SseEmitter emitter, SecurityContext context) {
        AiResponse record = saveInitialPending(queries, modelName,ResponseType.INDIVIDUAL);
        final Long responseId = record.getId();

        return CompletableFuture.supplyAsync(() -> {
            SecurityContextHolder.setContext(context);
            long startTime = System.currentTimeMillis();
            try {
                RecommendationAgent agent = AiServices.create(RecommendationAgent.class, model);
                // 1. Result 객체로 수신 (데이터 + 토큰 정보 포함)
                Result<AiRecommendationResponse> result = agent.generate(systemPrompt, question);
                AiRecommendationResponse aiOutput = result.content();

                AiRecommendationResponse finalResponse = new AiRecommendationResponse(// DB에서 가져온 ID
                        modelName,    // 메서드 파라미터로 받은 이름
                        aiOutput.recommendations(),
                        aiOutput.specGuide(),
                        aiOutput.finalWord()
                );

                // 2. 프론트엔드로 즉시 전송 (DTO 전송)
                sendSse(emitter, modelName + "_ANSWER", finalResponse);

                // 3. DB 저장 (Result 객체 통째로 넘김)
                saveCompletion(responseId, result,finalResponse, startTime);

                return result.content();
            } catch (Exception e) {
                log.error("{} 호출 에러: {}", modelName, e.getMessage());
                updateToFailed(responseId, e.getMessage());
                return null;
            } finally{
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
    public AiResponse saveInitialPending(Queries queries, String modelName , ResponseType type) {
        Models model = modelsRepository.findByName(modelName)
                .orElseThrow(() -> new RuntimeException("모델 정보를 찾을 수 없습니다: " + modelName));
        // 지성님의 AiResponse 엔티티 빌더를 사용하여 PENDING 상태로 저장
        AiResponse response = AiResponse.builder()
                .queries(queries)
                .model(model)
                .responseType(type)
                .build();
        return aiResponseRepository.save(response);
    }

    @Override
    @Transactional
    public <T> void saveCompletion(Long recordId, Result<T> result,T content, long startTime) {
        try {
            AiResponse record = aiResponseRepository.findByIdWithModel(recordId)
                    .orElseThrow(() -> new RuntimeException("저장할 레코드를 찾을 수 없습니다."));

            long latency = System.currentTimeMillis() - startTime;
            log.info("지연 시간 계산됨: {} ms", latency); // ⭐️ 로그로 확인!
            // 1. 메타데이터 구성 (지연 시간 + 토큰 정보)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("latency_ms", latency);

            if (result.tokenUsage() != null) {
                metadata.put("token_usage", Map.of(
                        "input_tokens", result.tokenUsage().inputTokenCount(),
                        "output_tokens", result.tokenUsage().outputTokenCount(),
                        "total_tokens", result.tokenUsage().totalTokenCount()
                ));
            }

            // 2. 객체를 JSON 문자열로 직렬화 (content 필드용)
            // objectMapper가 aiResponse가 어떤 객체든 알아서 JSON으로 구워줍니다.
            String jsonContent = objectMapper.writeValueAsString(content);

            log.info("엔티티에 전달될 최종 메타데이터: {}", metadata);
            // 3. 엔티티 비즈니스 로직 호출
            record.complete(jsonContent, metadata);

            // 4. DB 반영
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

    private String formatResponsesForReport(AiRecommendationResponse gpt, AiRecommendationResponse gemini, AiRecommendationResponse perp) {
        StringBuilder sb = new StringBuilder();

        if (gpt != null) appendModelOutput(sb, "GPT", gpt);
        if (gemini != null) appendModelOutput(sb, "GEMINI", gemini);
        if (perp != null) appendModelOutput(sb, "PERPLEXITY", perp);

        return sb.toString();
    }

    private void appendModelOutput(StringBuilder sb, String modelName, AiRecommendationResponse response) {
        sb.append("[").append(modelName).append(" 추천 제품]\n");
        for (ProductRecommendation rec : response.recommendations()) {
            sb.append("- 모델명: ").append(rec.modelName()).append("\n");
            sb.append("  추천대상: ").append(rec.targetAudience()).append("\n");
            sb.append("  선정이유: ").append(String.join(", ", rec.selectionReasons())).append("\n");
        }
        sb.append("스펙가이드: ").append(response.specGuide()).append("\n\n");
    }
}
