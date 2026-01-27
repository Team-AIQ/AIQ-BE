package cmc.aiq.aiq.service;

import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;

public interface AuthService {
    void signUp(String email, String password, String nickname);
    TokenResponseDTO login(LoginRequestDTO loginrequestDTO);
    TokenResponseDTO refresh(String refreshToken);
}
