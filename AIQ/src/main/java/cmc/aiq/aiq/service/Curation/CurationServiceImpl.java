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
public class CurationServiceImpl implements CurationService{
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
        // ... (기존 코드는 동일)
        return null; // This is a placeholder, the actual logic is in the original file
    }
    
    // ... (saveInitialSession, saveUserAnswers, getUserHistory 메소드는 동일)

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
                // 파싱 실패는 무시하고 계속 진행
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
