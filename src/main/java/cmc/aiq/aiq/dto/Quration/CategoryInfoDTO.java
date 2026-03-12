package cmc.aiq.aiq.dto.Quration;

import java.util.List;

public record CategoryInfoDTO(String categoryName,
                              String displayName,
                              List<CategoryAttributesDTO> attributes) {
}
// Repository에서 해당하는 컬럼만 빼오기 위한 DTO
