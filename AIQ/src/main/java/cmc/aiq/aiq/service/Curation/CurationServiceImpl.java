package cmc.aiq.aiq.service.Curation;

import cmc.aiq.aiq.domain.*;
import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
import cmc.aiq.aiq.domain.ENUM.ResponseType;
import cmc.aiq.aiq.dto.FinalReport.FinalReportResponse;
import cmc.aiq.aiq.dto.History.HistoryResponseDTO;
import cmc.aiq.aiq.dto.MultiAiDTO.AiRecommendationResponse;
import cmc.aiq.aiq.dto.Quration.*;
import cmc.aiq.aiq.repository.*;
import cmc.aiq.aiq.service.Credit.CreditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CurationServiceImpl implements CurationService {
    private final QueriesRepository queriesRepository;
    private final CategoryAttributesRepository categoryRepository;
    private final AiResponseRepository aiResponseRepository;
    private final EmbeddingModel embeddingModel;
    private final CurationAgent curationAgent;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final CurationSessionsRepository curationSessionsRepository;
    private final CreditService creditService;

    private static final double MATCH_THRESHOLD = 0.43;

    @Override
    @Transactional
    public CurationResponseDTO initiateCuration(CurationRequestDTO request) {
        log.info("1. 사용자 질문 수신: {}", request.getQuestion());
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        creditService.useCredit(user.getId(), CreditTransactionType.REPORT_GENERATION);
        log.info("크레딧 차감 시도: userId={}, type={}", user.getId(), CreditTransactionType.REPORT_GENERATION.name());

        Queries query = queriesRepository.save(Queries.builder()
                .user(user)
                .question(request.getQuestion())
                .build());

        log.info("2. 질문 벡터화 및 카테고리 검색 시작");
        float[] vector = embeddingModel.embed(request.getQuestion()).content().vector();

        Optional<CategoryDistanceResult> closestCategory = categoryRepository.findClosestCategory(vector);

        // ... (이하 기존 initiateCuration 로직은 그대로 유지)
        List<CategoryAttributesDTO> curationQuestions;
        String message;
        String categoryName;

        if (closestCategory.isPresent() && closestCategory.get().getDistance() <= MATCH_THRESHOLD) {
            CategoryDistanceResult closest = closestCategory.get();
            categoryName = closest.getCategoryName();
            List<CategoryAttributesDTO> attributes;
            try {
                attributes = objectMapper.readValue(closest.getAttributes(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("카테고리 데이터 파싱 오류", e);
            }
            CurationResult result = curationAgent.refineExisting(request.getQuestion(), attributes);
            curationQuestions = result.getQuestions();
            message = String.format("[%s] 카테고리에 최적화된 질문입니다.", closest.getDisplayName());
        } else {
            AiCategoryAnalysisDTO analysis = curationAgent.createNewCategory(request.getQuestion());
            categoryName = analysis.getCategoryName();
            var existingCategory = categoryRepository.findSimpleByCategoryName(categoryName);
            if (existingCategory.isPresent()) {
                var category = existingCategory.get();
                curationQuestions = curationAgent.refineExisting(request.getQuestion(), category.attributes()).getQuestions();
                message = String.format("[%s] 카테고리로 안내해 드릴게요.", category.displayName());
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
                    throw new RuntimeException("카테고리 저장 중 데이터 변환 에러", e);
                }
            }
        }
        saveInitialSession(user, query, curationQuestions, categoryName);
        log.info("[3] 큐레이션 질문 생성 완료. QueryID: {}", query.getId());
        return new CurationResponseDTO(query.getId(), categoryName, curationQuestions, message);
    }

    private void saveInitialSession(Users user, Queries query, List<CategoryAttributesDTO> questions, String categoryName) {
        CategoryAttributes category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        List<CurationUserAnswerDTO> sessionResults = questions.stream()
                .map(q -> new CurationUserAnswerDTO(q.getDisplayLabel(), q.getQuestionText(), q.getUserAnswer()))
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
        CurationSessions session = curationSessionsRepository.findByQueryId(request.getQueryId())
                .orElseThrow(() -> new RuntimeException("해당 질문에 대한 큐레이션 세션을 찾을 수 없습니다."));
        session.updateResults(request.getAnswers());
        curationSessionsRepository.save(session);
        log.info("QueryID {}에 대한 사용자 답변 저장 완료", request.getQueryId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryResponseDTO> getUserHistory(Long userId) {
        return queriesRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(q -> new HistoryResponseDTO(q.getId(), q.getQuestion(), q.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CurationResultDetailDTO getCurationResultDetail(Long userId, Long queryId) {
        Queries query = queriesRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("해당 기록을 찾을 수 없습니다."));

        if (!query.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 기록만 열람할 수 있습니다.");
        }

        List<AiResponse> allResponses = aiResponseRepository.findAllByQueriesId(queryId);

        FinalReportResponse finalReport = null;
        List<AiRecommendationResponse> individualReports = new ArrayList<>();

        for (AiResponse response : allResponses) {
            try {
                if (response.getResponseType() == ResponseType.FINAL_REPORT) {
                    finalReport = objectMapper.readValue(response.getContent(), FinalReportResponse.class);
                } else if (response.getResponseType() == ResponseType.INDIVIDUAL) {
                    AiRecommendationResponse individualReport = objectMapper.readValue(response.getContent(), AiRecommendationResponse.class);
                    individualReports.add(individualReport);
                }
            } catch (JsonProcessingException e) {
                log.error("보고서 파싱 실패: reportId={}, message={}", response.getId(), e.getMessage());
            }
        }

        if (finalReport == null && individualReports.isEmpty()) {
            throw new RuntimeException("분석된 리포트 데이터가 없습니다.");
        }

        return CurationResultDetailDTO.builder()
                .finalReport(finalReport)
                .individualReports(individualReports)
                .build();
    }
}
