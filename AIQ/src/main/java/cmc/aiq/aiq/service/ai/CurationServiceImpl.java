package cmc.aiq.aiq.service.ai;

import cmc.aiq.aiq.domain.CategoryAttributes;
import cmc.aiq.aiq.domain.Queries;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import cmc.aiq.aiq.dto.Quration.CategoryDistanceResult;
import cmc.aiq.aiq.dto.Quration.CurationRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationResponseDTO;
import cmc.aiq.aiq.repository.CategoryAttributesRepository;
import cmc.aiq.aiq.repository.QueriesRepository;
import cmc.aiq.aiq.repository.UsersRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CurationServiceImpl implements CurationService{
    private final QueriesRepository queriesRepository;
    private final CategoryAttributesRepository categoryRepository;
    private final EmbeddingModel embeddingModel; // DB 기반 설정 적용됨
    private final CurationAgent curationAgent;   // 이제 정상 작동하는 AI 비서
    private final UsersRepository usersRepository;

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

        List<CategoryAttributesDTO> curationQuestions;
        String message;
        String categoryName;

        // 4. 매칭 성공 여부 판단 (Threshold 로직)
        if (closestCategory.isPresent() && closestCategory.get().getDistance() <= MATCH_THRESHOLD) {
            CategoryDistanceResult closest = closestCategory.get(); // 꺼내기

            log.info("[2-A] 카테고리 매칭 성공: {} (Distance: {})",
                    closest.getDisplayName(), closest.getDistance());

            categoryName = closest.getCategoryName();
            message = String.format("[%s] 카테고리에 최적화된 질문 세트입니다.", closest.getDisplayName());

            // AI에게 기존 질문 정제 요청 (꺼낸 객체에서 attributes 전달)
            curationQuestions = curationAgent.refineQuestions(request.getQuestion(), closest.getAttributes());
        } else {
            double distance = closestCategory.map(CategoryDistanceResult::getDistance).orElse(1.0);
            log.info("[2-B] 매칭 실패 (Distance: {}). AI 동적 생성 모드 진입", distance);

            categoryName = "CUSTOM";
            message = "적절한 카테고리를 찾지 못해 AI가 맞춤형 질문을 생성했습니다.";

            // AI에게 새로운 질문 세트 생성 요청
            curationQuestions = curationAgent.generateNewQuestions(request.getQuestion());
        }

        log.info("[3] 큐레이션 질문 생성 완료. QueryID: {}", query.getId());
        return new CurationResponseDTO(query.getId(), categoryName, curationQuestions, message);
    }
}
