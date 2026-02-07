package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "gptModel")
public interface ReportAgent {

    @SystemMessage("{{systemPrompt}}") // DB에서 가져온 프롬프트가 이리로 들어갑니다.
    Result<FinalReportResponse> generateReport(
            @V("systemPrompt") String systemPrompt,
            @UserMessage("question") String userQuestion,
            @V("context") String context,
            @V("combinedResponses") String combinedResponses,
            @V("categoryName") String categoryName
    );
}
