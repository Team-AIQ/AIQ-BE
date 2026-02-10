package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.EmbeddingTestDto;
import cmc.aiq.aiq.dto.EmbeddingTestResponse;
import cmc.aiq.aiq.dto.Quration.CategoryDistanceResult;
import cmc.aiq.aiq.repository.CategoryAttributesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Optional;

@RestController
@RequestMapping("/api/test/embedding")
@RequiredArgsConstructor
@Log4j2
public class EmbeddingTestController {
    private final EmbeddingModel embeddingModel;
    private final CategoryAttributesRepository categoryRepository;

    // 서비스에서 사용하는 동일한 임계치
    private static final double MATCH_THRESHOLD = 0.43;

    @PostMapping("/similarity")
    public ResponseEntity<EmbeddingTestResponse> testSimilarity(@RequestBody String text) {
        log.info("유사도 테스트 시작: {}", text);

        // 1. 입력 텍스트 벡터화 (AI API 호출 없이 로컬 모델 사용 시 비용 0원)
        float[] vector = embeddingModel.embed(text).content().vector();

        // 2. DB에서 가장 가까운 벡터 검색
        Optional<CategoryDistanceResult> result = categoryRepository.findClosestCategory(vector);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CategoryDistanceResult closest = result.get();
        double distance = closest.getDistance();

        // 3. 결과 DTO 구성
        EmbeddingTestResponse response = EmbeddingTestResponse.builder()
                .inputText(text)
                .matchedCategory(closest.getCategoryName())
                .displayName(closest.getDisplayName())
                .distance(distance)
                .similarityScore(1 - distance) // 코사인 유사도 점수화
                .isPassed(distance <= MATCH_THRESHOLD)
                .build();

        log.info("테스트 결과 - 카테고리: {}, 거리: {}", response.getDisplayName(), distance);

        return ResponseEntity.ok(response);
    }
    @PostMapping("/compare")
    public ResponseEntity<EmbeddingTestDto.CompareResponse> compareTwoTexts(
            @RequestBody EmbeddingTestDto.CompareRequest request) {

        log.info("두 문장 비교 시작: [{}] vs [{}]", request.getText1(), request.getText2());

        // 1. 각 문장 벡터화
        float[] v1 = embeddingModel.embed(request.getText1()).content().vector();
        float[] v2 = embeddingModel.embed(request.getText2()).content().vector();

        // 2. 코사인 유사도 계산 (Manual Math)
        double similarity = calculateCosineSimilarity(v1, v2);
        double distance = 1.0 - similarity; // pgvector의 <=> 연산자와 동일한 값

        // 3. 수치에 따른 한줄평 작성
        String evaluation = (distance <= 0.43) ? "상당히 유사함 (MATCH 가능)"
                : (distance <= 0.55) ? "연관성 있음 (경계선)"
                : "관련성 낮음 (MISMATCH)";

        EmbeddingTestDto.CompareResponse response = EmbeddingTestDto.CompareResponse.builder()
                .text1(request.getText1())
                .text2(request.getText2())
                .distance(distance)
                .similarityScore(similarity)
                .evaluation(evaluation)
                .build();

        return ResponseEntity.ok(response);
    }
    private double calculateCosineSimilarity(float[] v1, float[] v2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += Math.pow(v1[i], 2);
            normB += Math.pow(v2[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
