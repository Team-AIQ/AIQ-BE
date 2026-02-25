package cmc.aiq.aiq.service.Curation;

import cmc.aiq.aiq.domain.*;
import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.Quration.*;
import cmc.aiq.aiq.repository.*;
import cmc.aiq.aiq.service.Credit.CreditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CurationServiceImpl implements CurationService{
    private final QueriesRepository queriesRepository;
    private final CategoryAttributesRepository categoryRepository;
    private final AiResponseRepository aiResponseRepository;
    private final EmbeddingModel embeddingModel;
    private final CurationAgent curationAgent;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final CurationSessionsRepository curationSessionsRepository;
    private final CreditService creditService; // CreditService 주입

    private static final double MATCH_THRESHOLD = 0.43;

    @Override
    @Transactional
    public CurationResponseDTO initiateCuration(CurationRequestDTO request) {
        log.info("1. 사용자 질문 수신: {}", request.getQuestion());
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // [추가] 크레딧 차감 로직
        creditService.useCredit(user.getId(), CreditTransactionType.REPORT_GENERATION);
        log.info("크레딧 차감 시도: userId={}, type={}", user.getId(), CreditTransactionType.REPORT_GENERATION.name());

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
                String textToEmbed = analysis.getDisplayName() + " " + analysis.getDescription();
                float[] newCategoryVector = embeddingModel.embed(textToEmbed).content().vector();
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
        saveInitialSession(user, query, curationQuestions , categoryName);
        log.info("[3] 큐레이션 질문 생성 완료. QueryID: {}", query.getId());
        return new CurationResponseDTO(query.getId(), categoryName, curationQuestions, message);
    }
    
    private void saveInitialSession(Users user, Queries query, List<CategoryAttributesDTO> questions , String categoryName) {
        log.info("CurationSession 초기 상태 저장 시작");

        CategoryAttributes category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));

        List<CurationUserAnswerDTO> sessionResults = questions.stream()
                .map(q -> new CurationUserAnswerDTO(
                        q.getDisplayLabel(),
                        q.getQuestionText(),
                        q.getUserAnswer()
                ))
                .toList();

        CurationSessions session = CurationSessions.builder()
                .user(user)
                .query(query)
                .categoryAttributes(category)
                .curationResults(sessionResults)
                .build();

        curationSessionsRepository.save(session);
    }

    @Override
    public void saveUserAnswers(CurationSubmitRequestDTO request) {
        log.info("대답 저장 요청 시작 - QueryID: {}, 답변 개수: {}",
                request.getQueryId(),
                (request.getAnswers() != null ? request.getAnswers().size() : 0));
        CurationSessions session = curationSessionsRepository.findByQueryId(request.getQueryId())
                .orElseThrow(() -> new RuntimeException("해당 질문에 대한 큐레이션 세션을 찾을 수 없습니다."));

        session.updateResults(request.getAnswers());
        log.info("업데이트 후 결과: {}", session.getCurationResults());
        curationSessionsRepository.save(session);

        log.info("QueryID {}에 대한 사용자 답변 저장 완료", request.getQueryId());
    }
    
    @Transactional
    @Override
    public List<HistoryResponseDTO> getUserHistory(Long userId) {
        return queriesRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(q -> new HistoryResponseDTO(
                        q.getId(),
                        q.getQuestion(),
                        q.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    
    @Transactional
    @Override
    public FinalReportResponse getFinalReportOnly(Long userId, Long queryId) {
        Queries query = queriesRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("해당 기록을 찾을 수 없습니다."));

        if (!query.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 보고서만 열람할 수 있습니다.");
        }

        AiResponse reportResponse = aiResponseRepository.findByQueriesIdAndResponseType(queryId, ResponseType.FINAL_REPORT)
                .orElseThrow(() -> new RuntimeException("최종 보고서가 생성되지 않은 질문입니다."));

        try {
            return objectMapper.readValue(reportResponse.getContent(), FinalReportResponse.class);
        } catch (JsonProcessingException e) {
            log.error("보고서 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("보고서 데이터를 읽는 중 오류가 발생했습니다.");
        }
    }
}
