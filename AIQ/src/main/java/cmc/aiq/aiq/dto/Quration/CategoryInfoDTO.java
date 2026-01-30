package cmc.aiq.aiq.dto.Quration;

import java.util.List;

public record CategoryInfoDTO(String categoryName,
                              String displayName,
                              List<CategoryAttributesDTO> attributes) {
}
