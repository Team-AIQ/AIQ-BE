package cmc.aiq.aiq.dto;

import lombok.Builder;
import lombok.Getter;

public class EmbeddingTestDto {
    @Getter
    public static class CompareRequest {
        private String text1;
        private String text2;
    }

    @Getter
    @Builder
    public static class CompareResponse {
        private String text1;
        private String text2;
        private double distance;        // 코사인 거리 (1 - Similarity)
        private double similarityScore; // 코사인 유사도 (0 ~ 1)
        private String evaluation;      // 수치에 대한 분석 한줄평
    }
}
