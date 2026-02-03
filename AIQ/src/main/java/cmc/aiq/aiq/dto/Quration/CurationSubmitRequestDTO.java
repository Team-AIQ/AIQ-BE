package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public static class AnswerItem {
        private String displayLabel;
        private String selectedAnswer;
    }
}
