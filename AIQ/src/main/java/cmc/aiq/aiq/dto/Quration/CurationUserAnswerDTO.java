package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurationUserAnswerDTO {
    private String displayLabel;
    private String questionText;
    private String selectedAnswer;
}
