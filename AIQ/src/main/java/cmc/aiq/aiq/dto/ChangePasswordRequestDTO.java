package cmc.aiq.aiq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangePasswordRequestDTO {

    @Schema(description = "현재 비밀번호", example = "currentPassword123!")
    private String currentPassword;

    @Schema(description = "새로운 비밀번호", example = "newPassword123!")
    private String newPassword;
}
