package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiCategoryAnalysisDTO {
    // 큐레이션 카테고리를 동적으로 생성할 경우 AI응답을 저장할 DTO
    private String categoryName;
    private String displayName;
    private List<CategoryAttributesDTO> questions;
}
