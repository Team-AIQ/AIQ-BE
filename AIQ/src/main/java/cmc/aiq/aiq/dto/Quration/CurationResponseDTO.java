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
    // 사용자에게 큐레이션 질문 생성 후 보내는 DTO
    private Long queryId;
    private String categoryName;
    private List<CategoryAttributesDTO> questions;
    private String message;
}
