package cmc.aiq.aiq.dto.Quration;

import java.util.List;

public interface CategoryDistanceResult {
    // 카테고리 정보와 거리 점수를 담기 위한 인터페이스 프로젝션

    Long getId();
    String getCategoryName();
    String getDisplayName();

    // 이 부분이 핵심! 우리가 SQL에서 'as distance'라고 이름 붙인 값이 여기 들어옵니다.
    Double getDistance();
//    List<CategoryAttributesDTO> getAttributes();
    String getAttributes();
}
