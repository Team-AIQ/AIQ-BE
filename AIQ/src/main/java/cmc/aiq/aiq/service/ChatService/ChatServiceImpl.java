package cmc.aiq.aiq.service.ChatService;

import cmc.aiq.aiq.domain.AiResponse;
import cmc.aiq.aiq.domain.ChatMessages;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.domain.ENUM.SenderType;
import cmc.aiq.aiq.domain.Models;
import cmc.aiq.aiq.dto.ChatDTO.ChatResponse;
import cmc.aiq.aiq.repository.AiResponseRepository;
import cmc.aiq.aiq.repository.ChatMessagesRepository;
import cmc.aiq.aiq.repository.ModelsRepository;
import cmc.aiq.aiq.repository.QueriesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

@Log4j2
public class ChatServiceImpl implements ChatService{
    private final QueriesRepository queriesRepository;
    private final AiResponseRepository aiResponseRepository; // 최종 리포트 추출용
    private final ChatMessagesRepository chatMessagesRepository;
    private final ModelsRepository modelsRepository;
    private final ChatAgent chatAgent;// 추천 질문 전용 LangChain4j 에이전트
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<String> initiateChatRoundZero(Long queryId, String modelName) {
        log.info("Query ID {}에 대한 0회차 추천 질문 생성 시작", queryId);

        // 1. 기존에 이미 0회차가 있는지 확인 (중복 방지)
        if (chatMessagesRepository.existsByQueriesIdAndRoundCount(queryId, 0)) {
            throw new RuntimeException("이미 초기 질문이 생성된 세션입니다.");
        }

        // 2. 최종 리포트(Final Report) 내용 가져오기
        // 가령 'GPT' 모델이 쓴 최종 리포트 레코드를 찾는 로직
        AiResponse finalReport = aiResponseRepository.findByQueriesIdAndResponseType(queryId , ResponseType.FINAL_REPORT)
                .orElseThrow(() -> new RuntimeException("참조할 최종 리포트를 찾을 수 없습니다."));

        // 3. AI에게 마중물 질문 3개 요청
        // chatAgent는 @UserMessage를 통해 질문 리스트(JSON)를 반환하도록 설계
        String suggestedJson = chatAgent.generateStarterQuestions(finalReport.getContent());

        List<String> questions;
        try {
            questions = objectMapper.readValue(suggestedJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("추천 질문 파싱 실패", e);
            questions = List.of("제품의 상세 스펙이 궁금하신가요?", "가성비가 가장 좋은 모델은 무엇인가요?", "A/S 정책에 대해 더 알아볼까요?");
        }

        // 4. ChatMessages 테이블에 0회차 레코드로 저장
        Models model = modelsRepository.findByName(modelName)
                .orElseThrow(() -> new RuntimeException("모델 정보를 찾을 수 없습니다."));

        ChatMessages starterMessage = ChatMessages.builder()
                .queries(finalReport.getQueries())
                .model(model)
                .senderType(SenderType.AI)
                .content("안녕하세요! 분석 보고서에 대해 더 궁금한 점이 있으신가요? 아래 질문들로 대화를 시작해보세요.")
                .roundCount(0)
                .suggestedQuestions(suggestedJson) // JSON 문자열 그대로 저장
                .build();

        chatMessagesRepository.save(starterMessage);

        return questions;
    }

    @Transactional
    public ChatResponse processUserChat(Long queryId, String userQuestion) {
        // 1. 현재 회차 확인 (최근 AI 답변의 roundCount 기준)
        ChatMessages lastAiMessage = chatMessagesRepository.findTopByQueriesIdOrderByCreatedAtDesc(queryId)
                .orElseThrow(() -> new RuntimeException("대화가 시작되지 않았습니다."));

        int nextRound = lastAiMessage.getRoundCount() + 1;
        if (nextRound > 4) throw new RuntimeException("최대 대화 횟수(4회)를 초과했습니다.");

        // 2. 맥락(Report + History) 수집
        String reportContent = aiResponseRepository.findByQueriesIdAndResponseType(queryId, ResponseType.FINAL_REPORT)
                .map(AiResponse::getContent).orElse("");

        String history = chatMessagesRepository.findAllByQueriesIdOrderByCreatedAtAsc(queryId)
                .stream()
                .map(m -> m.getSenderType() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        // 3. 사용자 질문 저장
        chatMessagesRepository.save(ChatMessages.builder()
                .queries(lastAiMessage.getQueries())
                .model(lastAiMessage.getModel())
                .senderType(SenderType.USER)
                .content(userQuestion)
                .roundCount(nextRound)
                .build());

        // 4. AI 답변 및 추천 질문 생성
        String rawResponse = chatAgent.generateAnswer(reportContent, history, userQuestion,nextRound);
        String answer;
        String suggestedQuestionsJson = "[]"; // 기본값은 빈 배열

        if (rawResponse != null && rawResponse.contains("===")) {
            String[] parts = rawResponse.split("===");
            answer = parts[0].trim();
            suggestedQuestionsJson = parts[1].trim();
        } else {
            log.warn("AI 응답에 구분자가 없습니다. 전체를 답변으로 처리합니다. QueryId: {}", queryId);
            answer = (rawResponse != null) ? rawResponse.trim() : "죄송합니다. 답변을 생성하지 못했습니다.";
        }

        List<String> nextQuestions = parseQuestions(suggestedQuestionsJson);

        // 5. AI 답변 저장
        chatMessagesRepository.save(ChatMessages.builder()
                .queries(lastAiMessage.getQueries())
                .model(lastAiMessage.getModel())
                .senderType(SenderType.AI)
                .content(answer)
                .roundCount(nextRound)
                .suggestedQuestions(suggestedQuestionsJson)
                .build());

        return new ChatResponse(answer, nextQuestions, nextRound, nextRound == 4);
    }
    private List<String> parseQuestions(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return List.of();
        }

        // 1. ```json ... ``` 같은 마크다운 코드 블록 제거
        String cleanedJson = json.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)\\s*```$", "");
        try {
            return objectMapper.readValue(json.trim(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(); // 파싱 실패 시 빈 리스트
        }
    }
}
