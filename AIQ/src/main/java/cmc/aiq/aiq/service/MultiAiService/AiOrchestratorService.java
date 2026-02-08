package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.dto.MultiAiDTO.AiRecommendationResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.Result;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AiOrchestratorService {
    void executeParallelAi(Long queryId, SseEmitter emitter);
//    Map<String, AiRecommendationResponse> testExecuteParallelAi(Long queryId, String userQuestion);
    CompletableFuture<AiRecommendationResponse> callAi(ChatLanguageModel model, String modelName, String systemPrompt,
                                                   String question, Queries queries, SseEmitter emitter, SecurityContext context);
    void updateToFailed(Long recordId, String error);
    AiResponse saveInitialPending(Queries queries, String modelName, ResponseType type);
    <T> void saveCompletion(Long recordId, Result<T> result, long startTime);
}
