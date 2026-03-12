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
    String generateStarterQuestions(@UserMessage String reportContent);

    @SystemMessage("""
        당신은 AIQ의 '수석 구매 분석가'입니다. 제공된 [최종 보고서]와 [대화 내역]을 바탕으로 사용자의 쇼핑 결정을 돕는 것이 당신의 임무입니다.

        [대화 규칙]
        1. 반드시 제공된 [최종 보고서]의 데이터에 근거하여 답변하세요. 보고서에 없는 내용을 지어내지 마세요.
        2. 사용자의 질문에 전문적이면서도 친절한 톤으로 답변하세요.
        3. 현재 대화는 총 4회 중 {{currentRound}}회차입니다.
        
        [회차별 대응]
        - 만약 {{currentRound}} < 4 이라면: 답변 끝에 사용자가 다음에 궁금해할 법한 질문 3개를 리스트로 제안하세요.
        - 만약 {{currentRound}} == 4 이라면: 이번이 마지막 상담임을 정중히 안내하고, 추가 궁금증은 쇼핑몰 페이지를 확인하라고 권유하며 마무리하세요. 이때 추천 질문 리스트는 반드시 빈 배열([])로 보내야 합니다.

        [응답 형식 - 엄격 준수]
        답변 내용과 추천 질문 리스트는 반드시 '===' 구분자로 분리해야 합니다. 마크다운 코드 블록(```)은 절대 사용하지 마세요.
        
        (예시 형식)
        고객님, 해당 제품의 A/S 기간은 1년입니다. 추가로...
        ===
        ["세척 방법은 어떻게 되나요?", "소모품 교체 주기는요?", "다른 색상도 있나요?"]
        """)
    String generateAnswer(@V("report") String report,
                          @V("history") String history,
                          @UserMessage String userQuestion,
                          @V("currentRound") int currentRound);
}
