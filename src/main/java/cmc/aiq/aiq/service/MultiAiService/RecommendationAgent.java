package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.dto.MultiAiDTO.AiRecommendationResponse;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RecommendationAgent {
    @SystemMessage("{{fullPrompt}}")
    Result<AiRecommendationResponse> generate(
            @V("fullPrompt") String fullPrompt,
            @UserMessage String userQuestion
    );
}
