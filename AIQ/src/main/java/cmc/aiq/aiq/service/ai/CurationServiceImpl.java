package cmc.aiq.aiq.service.ai;

import cmc.aiq.aiq.domain.CategoryAttributes;
import cmc.aiq.aiq.domain.CurationSessions;
import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.Quration.*;
import cmc.aiq.aiq.repository.CategoryAttributesRepository;
import cmc.aiq.aiq.repository.CurationSessionsRepository;
import cmc.aiq.aiq.repository.QueriesRepository;
import cmc.aiq.aiq.repository.UsersRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CurationServiceImpl implements CurationService{
    private final QueriesRepository queriesRepository;
    private final CategoryAttributesRepository categoryRepository;
    private final EmbeddingModel embeddingModel;
    private final CurationAgent curationAgent;   // 이제 정상 작동하는 AI 비서
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final CurationSessionsRepository curationSessionsRepository;

    private static final double MATCH_THRESHOLD = 0.35;


    @Override
    @Transactional
    public CurationResponseDTO initiateCuration(CurationRequestDTO request) {
        log.info("1. 사용자 질문 수신 및 저장: {}", request.getQuestion());
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        Queries query = queriesRepository.save(Queries.builder()
                        .user(user)
                        .question(request.getQuestion())
                        .build());

        log.info("2. 질문 벡터화 및 카테고리 검색 시작");
        float[] vector = embeddingModel.embed(request.getQuestion()).content().vector();

        Optional<CategoryDistanceResult> closestCategory = categoryRepository.findClosestCategory(vector);

        if (closestCategory.isPresent()) {
            double actualDistance = closestCategory.get().getDistance();
            String catName = closestCategory.get().getDisplayName();

            log.info(">>>> [유사도 분석 결과] <<<<");
            log.info("검색된 가장 가까운 카테고리: {}", catName);
            log.info("측정된 거리(Distance): {}", String.format("%.4f", actualDistance));
            log.info("설정된 임계치(Threshold): {}", MATCH_THRESHOLD);

            if (actualDistance <= MATCH_THRESHOLD) {
                log.info("결과: [MATCH] 임계치 이내입니다. 기존 카테고리를 사용합니다.");
            } else {
                log.info("결과: [MISMATCH] 임계치를 초과했습니다. AI 분석(신규 생성)으로 넘어갑니다.");
            }
        } else {
            log.info(">>>> [유사도 분석 결과] DB에 비교할 카테고리가 전혀 없습니다.");
        }
        // --- 유사도 거리 확인 로그 끝 ---

        List<CategoryAttributesDTO> curationQuestions;
        String message;
        String categoryName;

        if (closestCategory.isPresent() && closestCategory.get().getDistance() <= MATCH_THRESHOLD) {
            CategoryDistanceResult closest = closestCategory.get();
            categoryName = closest.getCategoryName();
            List<CategoryAttributesDTO> attributes;
            try {
                attributes = objectMapper.readValue(closest.getAttributes(),
                        new TypeReference<List<CategoryAttributesDTO>>() {});
            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 중 에러 발생: {}", e.getMessage());
                throw new RuntimeException("카테고리 데이터를 읽는 중 오류가 발생했습니다.");
            }

            CurationResult result = curationAgent.refineExisting(request.getQuestion(), attributes);
            curationQuestions = result.getQuestions();
            message = String.format("[%s] 카테고리에 최적화된 질문입니다.", closest.getDisplayName());
        } else {
            AiCategoryAnalysisDTO analysis = curationAgent.createNewCategory(request.getQuestion());
            log.info("카테고리를 찾지 못했습니다. 새로운 카테고리를 생성합니다..");
            categoryName = analysis.getCategoryName();

            var existingCategory = categoryRepository.findSimpleByCategoryName(categoryName);

            if (existingCategory.isPresent()) {
                var category = existingCategory.get();

                List<CategoryAttributesDTO> storedAttributes = category.attributes();

                CurationResult refined = curationAgent.refineExisting(request.getQuestion(), storedAttributes);

                curationQuestions = refined.getQuestions();
                message = String.format("[%s] 카테고리로 안내해 드릴게요.", category.displayName());

                log.info("유사도는 낮았지만 기존 카테고리({}) 명칭이 일치하여 재사용합니다.", categoryName);
            } else {
                curationQuestions = analysis.getQuestions();
                message = "새로운 쇼핑 분야를 발견하여 맞춤 질문을 구성했습니다.";

                float[] newCategoryVector = embeddingModel.embed(analysis.getDisplayName()).content().vector();

                try {
                    categoryRepository.insertCategoryWithVector(
                            analysis.getCategoryName(),
                            analysis.getDisplayName(),
                            Arrays.toString(newCategoryVector),
                            objectMapper.writeValueAsString(curationQuestions),
                            true
                    );
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("카테고리 저장 중 데이터 변환 에러!");
                }
            }
        }
        saveInitialSession(user, query, curationQuestions);
        log.info("[3] 큐레이션 질문 생성 완료. QueryID: {}", query.getId());
        return new CurationResponseDTO(query.getId(), categoryName, curationQuestions, message);
    }
    private void saveInitialSession(Users user, Queries query, List<CategoryAttributesDTO> questions) {
        log.info("CurationSession 초기 상태 저장 시작");

        // CategoryAttributesDTO -> CurationUserAnswerDTO 변환
        // 지성님의 DTO 구조(display_label, question_text, selected_answer)에 맞게 매핑합니다.
        List<CurationUserAnswerDTO> sessionResults = questions.stream()
                .map(q -> new CurationUserAnswerDTO(
                        q.getDisplay_label(),
                        q.getQuestion_text(),
                        q.getUser_answer() // AI가 추출한 대답을 selected_answer로 저장
                ))
                .toList();

        CurationSessions session = CurationSessions.builder()
                .user(user)
                .query(query)
                .curationResults(sessionResults)
                .build();

        curationSessionsRepository.save(session);
    }
}
