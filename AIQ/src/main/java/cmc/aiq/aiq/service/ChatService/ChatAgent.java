package cmc.aiq.aiq.service.ChatService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "gptModel")
public interface ChatAgent {
    @SystemMessage("""
        당신은 구매 분석 보고서를 기반으로 사용자의 궁금증을 유도하는 '질문 가이드'입니다.
        제공된 [보고서 내용]을 바탕으로 사용자가 가장 흥미로워할 만한 질문 3개를 선정하세요.
        
        - 질문은 짧고 명확해야 합니다.
        - 반드시 ["질문1", "질문2", "질문3"] 형식의 JSON 배열로만 응답하세요.
        """)
    String generateStarterQuestions(@V("report") String reportContent);

    @SystemMessage("""
        당신은 구매 분석 보고서를 바탕으로 사용자와 대화하는 수석 분석가입니다.
        
        [지침]
        1. 제공된 [보고서 내용]과 [이전 대화]를 바탕으로 논리적이고 친절하게 답변하세요.
        2. 답변이 끝나면, 사용자가 다음에 궁금해할 만한 질문 3개를 선정하세요.
        3. 응답은 반드시 아래 형식을 엄수하세요:
           답변 내용...
           ===
           ["질문1", "질문2", "질문3"]
        """)
    String generateAnswer(@V("report") String report,
                          @V("history") String history,
                          @UserMessage String userQuestion);
}
