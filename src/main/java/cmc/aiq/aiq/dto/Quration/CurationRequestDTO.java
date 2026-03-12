package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurationRequestDTO {
    // 사용자가 질문 했을 때 받는 DTO
    private Long userId;
    private String question;
}
