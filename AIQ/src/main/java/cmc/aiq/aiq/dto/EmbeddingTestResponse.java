package cmc.aiq.aiq.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddingTestResponse {
    private String inputText;        // 입력한 텍스트
    private String matchedCategory;  // 가장 유사한 카테고리 명칭
    private String displayName;     // 표시용 이름
    private double distance;         // 계산된 거리 (작을수록 유사)
    private double similarityScore;  // 유사도 점수 (1 - distance)
    private boolean isPassed;
}
