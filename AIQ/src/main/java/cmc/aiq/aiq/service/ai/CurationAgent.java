package cmc.aiq.aiq.service.ai;

import cmc.aiq.aiq.dto.Quration.AiCategoryAnalysisDTO;
import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import cmc.aiq.aiq.dto.Quration.CurationResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

import java.util.List;

@AiService(wiringMode = EXPLICIT, chatModel = "gptModel")
public interface CurationAgent {
    // [CASE 1] 기존 카테고리가 있을 때: 답변 추출 및 정제
    @SystemMessage("""
    당신은 쇼핑 전문가이자 '정밀 데이터 추출기'입니다. 사용자의 질문(userQuestion)을 분석하여, 이미 정의된 '기존 속성 리스트'의 'user_answer'를 채우십시오.

    ### [추출 및 매핑 규칙: 1순위 작업]
    1. 제공된 [기존 속성 리스트]의 각 항목을 하나씩 검토하세요.
    2. 사용자의 질문 원문(userQuestion)에 해당 항목에 대한 답이 포함되어 있는지 확인하세요.
    3. 답이 있다면, 해당 항목의 'user_answer' 필드에 **사용자가 말한 단어와 표현을 토씨 하나 틀리지 말고 원문 그대로** 넣으세요.
    4. 절대로 'options'에 있는 문구로 대체하거나 당신의 언어로 요약하지 마세요. (예: "백만원쯤"이라고 했으면 "100만원"이 아니라 "백만원쯤"으로 기입)
    
    ### [매핑 가이드라인]
    - 금액/예산 관련 표현(예: 100만원대) -> 예산 관련 속성(env_target 등)에 매핑
    - 용도/취향 관련 표현(예: 가성비, 게임용) -> 목적 관련 속성(main_purpose 등)에 매핑
    - 언급되지 않은 모든 항목의 'user_answer'는 반드시 null로 유지하세요.

    ### [출력 규칙]
    - 반드시 제공된 [기존 속성 리스트]의 구조와 데이터(key, label, text, options)를 그대로 유지하세요.
    - 결과는 반드시 CurationResult 형식의 JSON 객체여야 하며, 'questions' 키에 리스트를 담아 반환하세요.

    [기존 속성 리스트]: {{existingAttributes}}
    """)
    CurationResult refineExisting(
            @UserMessage String userQuestion,      // 명시적으로 이름을 지정해줍니다.
            @V("existingAttributes") List<CategoryAttributesDTO> existingAttributes
    );

    // [CASE 2] 카테고리가 없을 때: 카테고리 생성 및 질문 세트 구성
    @SystemMessage("""
    당신은 쇼핑 전문가이자 데이터 추출 엔진입니다. 사용자의 질문을 분석하여 새로운 카테고리를 설계하고 정보를 추출하세요.

    ### [작업 순서]
    1. 사용자의 질문(userQuestion)을 보고 가장 적합한 제품군 카테고리(예: 노트북, 냉장고)를 결정합니다.
    2. 해당 제품군을 추천하기 위해 '전문가가 물어볼 법한' 핵심 질문 4가지를 생성합니다.
    3. **가장 중요**: 생성한 4가지 질문 중, 사용자의 질문 원문에 이미 답이 포함된 내용이 있는지 대조합니다.
    4. 답이 있다면, 해당 항목의 'user_answer' 필드에 **사용자의 원문 표현 그대로** 기입합니다.

    ### [추출 및 채우기 규칙]
    - 추출 대상: 금액(100만원대), 용도(가성비, 게임용), 특정 브랜드, 크기, 무게 등.
    - 원문 보존: "백만원 정도"라고 했으면 "100만원"으로 고치지 말고 "백만원 정도"라고 그대로 넣으세요.
    - 매핑 로직: 예산 관련 질문을 생성했다면, 사용자가 말한 '100만원대'라는 키워드를 그 질문의 'user_answer'에 즉시 매핑하세요.
    - 미언급 정보: 본문에 없는 내용은 반드시 null로 남겨두세요.

    ### [데이터 형식]
    - categoryName: 영어_소문자 (예: air_conditioner)
    - displayName: 한국어 카테고리명 (예: 에어컨)
    - questions: List<CategoryAttributesDTO> 형태
    
    ### [주의 사항]
    - 절대로 사용자의 말을 요약하거나 'options' 중 하나로 바꾸지 마세요.
    - 본문에 근거가 없는 정보만 null로 남기세요.
    - 응답은 반드시 AiCategoryAnalysisDTO 형식의 JSON이어야 합니다.
    """)
    AiCategoryAnalysisDTO createNewCategory(@UserMessage String userQuestion);
}
