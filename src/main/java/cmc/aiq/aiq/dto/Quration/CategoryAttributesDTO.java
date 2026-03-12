package cmc.aiq.aiq.dto.Quration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoryAttributesDTO implements Serializable {
    // 사용자에게 보낼 AI 큐레이션 질문 양식
    @JsonProperty("attribute_key")
    private String attributeKey;

    @JsonProperty("display_label")
    private String displayLabel;

    @JsonProperty("question_text")
    private String questionText;

    @JsonProperty("options")
    private List<String> options;
    private String userAnswer;
}
