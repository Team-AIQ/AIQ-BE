package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryAttributesDTO implements Serializable {
    // 사용자에게 보낼 AI 큐레이션 질문 양식
    private String attributeKey;
    private String displayLabel;
    private String questionText;
    private List<String> options;
    private String userAnswer;
}
