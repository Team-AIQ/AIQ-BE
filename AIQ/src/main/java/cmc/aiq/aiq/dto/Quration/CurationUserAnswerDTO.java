package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurationUserAnswerDTO {
    private String display_label;
    private String question_text;
    private String selected_answer;
}
