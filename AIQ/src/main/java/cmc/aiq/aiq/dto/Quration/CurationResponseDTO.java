package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurationResponseDTO {
    private Long queryId;
    private String categoryName;
    private List<CategoryAttributesDTO> questions;
    String message;


}
