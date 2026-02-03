package cmc.aiq.aiq.dto.Quration;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CurationSubmitRequestDTO {
    private Long queryId;
    private List<AnswerItem> answers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AnswerItem {
        private String displayLabel;
        private String userAnswer;
    }
}
