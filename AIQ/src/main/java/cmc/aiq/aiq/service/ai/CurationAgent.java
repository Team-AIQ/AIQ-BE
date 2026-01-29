package cmc.aiq.aiq.service.ai;

import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface CurationAgent {
    /**
     * 3번 로직: 기존 카테고리 질문을 유저 맥락에 맞게 정제
     */
    @SystemMessage("""
        당신은 최고의 쇼핑 큐레이터입니다. 
        제공된 카테고리 질문 세트를 사용자의 질문 의도에 맞춰 가장 적절한 순서와 말투로 정제하여 JSON 배열로 반환하세요.
        반드시 AttributeDto 형식을 지켜야 합니다.
        """)
    List<CategoryAttributesDTO> refineQuestions(@UserMessage String userQuestion, List<CategoryAttributesDTO> dbAttributes);

    /**
     * 4번 로직: 카테고리가 없을 때 새로운 질문 세트 생성
     */
    @SystemMessage("""
        사용자의 질문에 맞는 적절한 쇼핑 카테고리가 없습니다. 
        사용자의 의도를 파악하여 구매 결정을 돕기 위한 4개의 필수 질문 세트를 생성하세요.
        결과는 반드시 AttributeDto의 JSON 배열 형식이어야 합니다.
        """)
    List<CategoryAttributesDTO> generateNewQuestions(@UserMessage String userQuestion);
}
