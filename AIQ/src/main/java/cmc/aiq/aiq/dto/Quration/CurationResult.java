package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurationResult {
    //LangChain4j는 내부적으로 AI에게 이런 JSON 형식으로 답해줘 라고 지시하는 JSON Schema를 자동으로 만드는데
    //java.util.List는 런타임에 타입 정보가 사라지는 특성이 있어 명확한 클래스로 주기 위해 작성함
    private List<CategoryAttributesDTO> questions;
}
