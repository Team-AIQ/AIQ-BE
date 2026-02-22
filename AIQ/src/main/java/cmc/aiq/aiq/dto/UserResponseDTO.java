package cmc.aiq.aiq.dto;

import cmc.aiq.aiq.domain.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {

    @Schema(description = "사용자 고유 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "AIQ사용자")
    private String nickname;

    @Schema(description = "현재 보유 크레딧", example = "100")
    private Long currentCredits;

    public static UserResponseDTO from(Users user) {
        return UserResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .currentCredits(user.getCurrentCredits())
                .build();
    }
}
