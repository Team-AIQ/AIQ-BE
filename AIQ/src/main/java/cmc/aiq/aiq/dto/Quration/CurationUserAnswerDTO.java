package cmc.aiq.aiq.dto.Quration;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CurationUserAnswerDTO {
    // AI가 사용자 대답을 자동으로 채워준 후 CurationSessions에 사용자 대답을 저장할 때 쓰는 DTO
    private String displayLabel;
    private String questionText;
    private String selectedAnswer;

    public void updateSelectedAnswer(String answer) {
        this.selectedAnswer = answer;
    }
}
