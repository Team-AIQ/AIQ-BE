package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiCategoryAnalysisDTO {
    private String categoryName;
    private String displayName;
    private List<CategoryAttributesDTO> questions;
}
