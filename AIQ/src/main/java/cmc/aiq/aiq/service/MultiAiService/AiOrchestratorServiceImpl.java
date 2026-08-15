package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.CurationSessions;
import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.domain.Models;
import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.FinalReport.TopProduct;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
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
    @Value("${ai.api.test-mode:false}")
    private boolean isTestMode;

    @Override
    @Transactional
    public void executeParallelAi(Long queryId, List<String> selectedModels, SseEmitter emitter) {
        // [추가] 최종 보고서가 이미 존재하는지 확인하는 방어 로직
        if (aiResponseRepository.existsByQueriesIdAndResponseType(queryId, ResponseType.FINAL_REPORT)) {
            String errorMessage = "이미 해당 질문에 대한 최종 보고서가 존재합니다. queryId: " + queryId;
            log.warn(errorMessage);
            sendSse(emitter, "ERROR", "이미 생성된 보고서입니다. 히스토리에서 확인해주세요.");
            emitter.complete(); // 이미 존재하는 경우, SSE 연결을 즉시 종료
            return;
        }

        SecurityContext mainContext = SecurityContextHolder.getContext();
        
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

                        FinalReportResponse enrichedReport;
                        Result<FinalReportResponse> reportResult = null; // 테스트 모드일 때는 Result가 없으므로 null 처리

                        // ★ [추가] 테스트 모드일 경우 최종 보고서와 이미지 검색을 생략하고 더미 반환
                        if (isTestMode) {
                            log.info("[테스트 모드] 최종 보고서 AI 호출 및 이미지 검색을 생략하고 상세한 더미 데이터를 반환합니다.");

                            // 1등 제품
                            TopProduct product1 = TopProduct.builder()
                                    .rank(1)
                                    .productName("쌤소나이트 아메리칸 투어리스터 큐리오 20인치")
                                    .productCode("12345678")
                                    .price("129,000원")
                                    .productImage("http://shop1.phinf.naver.net/20250314_286/1741920695656MEpPW_JPEG/28489755542263275_147561912.jpg")
                                    .specs(Map.of(
                                            "크기", "20인치",
                                            "소재", "폴리카보네이트",
                                            "바퀴", "저소음 더블 휠",
                                            "내부", "양쪽 지퍼 밀폐형"
                                    ))
                                    .lowestPriceLink("https://search.shopping.naver.com/search/all?query=쌤소나이트%20아메리칸%20투어리스터%20큐리오%2020인치")
                                    .comparativeAnalysis("1등 제품은 높은 내구성과 저소음 휠을 제공하여 이동 중 편리함을 극대화했습니다. 2등 제품에 비해 소재의 내구성과 바퀴의 저소음 기능이 우수합니다.")
                                    .build();

                            // 2등 제품
                            TopProduct product2 = TopProduct.builder()
                                    .rank(2)
                                    .productName("샤오미 90분 캐리어 20인치")
                                    .productCode("87654321")
                                    .price("115,000원")
                                    .productImage("https://img.theqoo.net/img/TEsfz.jpg")
                                    .specs(Map.of(
                                            "크기", "20인치",
                                            "소재", "ABS+PC",
                                            "바퀴", "이중 휠",
                                            "내부", "양쪽 지퍼 밀폐형"
                                    ))
                                    .lowestPriceLink("https://search.shopping.naver.com/search/all?query=샤오미%2090분%20캐리어%2020인치")
                                    .comparativeAnalysis("2등 제품은 가격 대비 훌륭한 옵션을 제공하지만, 1등 제품에 비해 바퀴의 저소음 성능이 다소 부족합니다.")
                                    .build();

                            // 3등 제품
                            TopProduct product3 = TopProduct.builder()
                                    .rank(3)
                                    .productName("리뽀 캐리어 나노 클래식 20인치")
                                    .productCode("11223344")
                                    .price("98,000원")
                                    .productImage("https://img.wizwid.com/PImg/703101/bsc/703101542.jpg")
                                    .specs(Map.of(
                                            "크기", "20인치",
                                            "소재", "ABS",
                                            "바퀴", "싱글 휠",
                                            "내부", "양쪽 지퍼 밀폐형"
                                    ))
                                    .lowestPriceLink("https://search.shopping.naver.com/search/all?query=리뽀%20캐리어%20나노%20클래식%2020인치")
                                    .comparativeAnalysis("3등 제품은 경제적인 선택이지만, 1등과 2등 제품에 비해 내구성과 바퀴의 소음 저감 기능이 제한적입니다.")
                                    .build();

                            // 최종 리포트 데이터 조립
                            enrichedReport = FinalReportResponse.builder()
                                    .consensus("사용자님이 '기내용 20인치'라는 사이즈와 '파손 방지 튼튼함'을 중요하게 언급하신 부분에 대해, 모든 AI 모델은 내구성과 이동의 용이성이 반드시 뒷받침되어야 한다고 판단했습니다. 따라서 이번 추천에서는 내구성 있는 소재와 저소음 더블 휠을 필수 기준으로 설정했습니다.")
                                    .decisionBranches("내구성 및 저소음 휠 중심 (Perplexity) vs 가격 효율성 및 디자인 중심 (Gemini, GPT)")
                                    .aiqRecommendationReason("사용자의 요구를 종합적으로 분석한 결과, 1위로 선정된 제품은 튼튼한 내구성, 저소음 더블 휠, 그리고 사용자의 예산 범위 내에서 최적의 가치를 제공하는 제품입니다. 특히, 잦은 이동 중에도 안전한 사용을 보장하는 내구성이 돋보입니다.")
                                    .topProducts(List.of(product1, product2, product3))
                                    .finalWord("이동의 편리함과 내구성을 동시에 충족시키는 제품을 선택함으로써, 다양한 여행 상황에서 후회 없는 사용 경험을 누리시길 바랍니다.")
                                    .build();

                            Thread.sleep(1500); // 1.5초 대기로 자연스러운 로딩 연출
                        } else {
                            // ★ 기존 실제 AI 호출 및 이미지 검색 로직
                            String systemPromptTemplate = promptManager.getProcessedPrompt("REPORT_AGENT_SYSTEM", Map.of());
                            reportResult = reportAgent.generateReport(
                                    systemPromptTemplate,
                                    userQuestion,
                                    curationContext,
                                    combinedText,
                                    categoryName
                            );

                            FinalReportResponse rawReport = reportResult.content();

                            log.info("제품 이미지 검색 시작...");
                            enrichedReport = reportEnrichmentService
                                    .enrichReportWithImages(rawReport)
                                    .join();
                        }

                        long reportStartTime = System.currentTimeMillis();
                        AiResponse reportRecord = saveInitialPending(queries, "GPT", ResponseType.FINAL_REPORT);

                        // reportResult는 테스트 모드일 때 null로 넘어가게 됨
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
                if (isTestMode) {
                    log.info("[테스트 모드] {} API 호출을 생략하고 상세한 더미 데이터를 반환합니다.", modelName);

                    // 첫 번째 추천 제품: Sony SRS-XB33
                    ProductRecommendation product1 = new ProductRecommendation(
                            "Sony SRS-XB33",
                            "SRSXB33/B",
                            "영화 감상/한 장소에서 사용/중간 크기 선호/야외 환경",
                            List.of(
                                    "지성님이 원하시는 '영화 감상'을 위한 균형 잡힌 사운드를 제공하며, 중저음이 강화되어 영화의 몰입감을 더합니다.",
                                    "Wi-Fi 연결을 계획하신다는 조건에 부합하여, 안정적인 연결성과 편리한 스트리밍 기능을 만족시킵니다.",
                                    "야외에서 주로 사용하실 계획이라면, 이 모델의 내구성 있는 디자인과 방수 기능은 필수입니다.",
                                    "디자인 중요성은 낮다고 하셨지만, 이 스피커의 세련된 외관은 다양한 환경에 잘 어울립니다."
                            )
                    );

                    // 두 번째 추천 제품: Sony SRS-XB43
                    ProductRecommendation product2 = new ProductRecommendation(
                            "Sony SRS-XB43",
                            "SRSXB43/B",
                            "영화 감상/한 장소에서 사용/중간 크기 선호/야외 환경",
                            List.of(
                                    "더 강력한 사운드와 넓은 사운드 스테이지로, 영화의 디테일을 놓치지 않고 전해줍니다.",
                                    "Wi-Fi 연결을 통해 손쉽게 여러 기기와 연결 가능하며, 영화 감상을 위한 최적의 조건을 제공합니다.",
                                    "배터리 수명은 중요하지 않으시다 하셨지만, 강력한 내구성으로 야외에서도 신뢰할 수 있는 성능을 발휘합니다.",
                                    "Sony의 기술력이 집약된 모델로, 한 번 구매하면 오랫동안 후회 없는 선택이 될 것입니다."
                            )
                    );

                    // 세 번째 추천 제품: Sony SRS-RA3000
                    ProductRecommendation product3 = new ProductRecommendation(
                            "Sony SRS-RA3000",
                            "SRSRA3000/H",
                            "영화 감상/한 장소에서 사용/중간 크기 선호/야외 환경",
                            List.of(
                                    "360 Reality Audio 기술로 영화의 사운드를 입체적으로 재현하여, 더욱 몰입감 있는 감상을 제공합니다.",
                                    "Wi-Fi로 연결하여 여러 스트리밍 서비스와 손쉽게 연동됩니다, 영화 감상에 최적화된 환경을 만듭니다.",
                                    "중간 크기의 디자인으로 공간을 차지하지 않으면서도 충분한 출력과 음질을 제공합니다.",
                                    "Sony의 프리미엄 모델 중 하나로, 품질과 성능에서 타협하지 않는 선택입니다."
                            )
                    );

                    // 스펙 가이드 작성
                    String specGuide = "스피커를 영화 감상용으로 선택할 때는, 360도 서라운드 사운드 지원 여부, Wi-Fi 연결성, IPX 등급의 방수 기능, 그리고 안정적인 내구성이 중요합니다. 특히, 야외에서 사용할 경우, 내구성과 방수 기능은 필수이며, 영화의 몰입감을 위한 사운드 스테이지 확보도 필수적입니다.";

                    // 최종 더미 응답 객체 조립
                    AiRecommendationResponse dummyResponse = new AiRecommendationResponse(
                            modelName,
                            List.of(product1, product2, product3),
                            specGuide
                    );

                    // 프론트엔드로 SSE 전송
                    sendSse(emitter, modelName + "_ANSWER", dummyResponse);

                    // DB에도 가짜 결과 저장 (Result 객체가 없으므로 null 전달)
                    saveCompletion(responseId, null, dummyResponse, startTime);

                    // 1.5초 정도 기다렸다가 반환해서, 실제 API 통신처럼 자연스러운 로딩 연출
                    Thread.sleep(1500);

                    return dummyResponse;
                }
                RecommendationAgent agent = AiServices.create(RecommendationAgent.class, model);
                Result<AiRecommendationResponse> result = agent.generate(systemPrompt, question);
                AiRecommendationResponse aiOutput = result.content();

                AiRecommendationResponse finalResponse = new AiRecommendationResponse(
                        modelName,
                        aiOutput.recommendations(),
                        aiOutput.specGuide()
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

            // ★ [수정] result가 null이 아닐 때만 토큰 사용량을 기록하도록 방어 코드 추가
            if (result != null && result.tokenUsage() != null) {
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
