package cmc.aiq.aiq.service;

import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import jakarta.mail.MessagingException;

public interface AuthService {
    void signUp(SignUpRequestDTO request);
    TokenResponseDTO login(LoginRequestDTO loginrequestDTO);
    TokenResponseDTO refresh(String refreshToken);
    void sendResetCode(String email) throws MessagingException;
    String verifyResetCode(String email, String code);
    void resetPassword(String resetToken, String newPassword);
}
