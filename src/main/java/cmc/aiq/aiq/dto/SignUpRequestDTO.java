package cmc.aiq.aiq.dto;

import lombok.Data;

@Data
public class SignUpRequestDTO {
    private String email;
    private String password;
    private String nickname;
}
