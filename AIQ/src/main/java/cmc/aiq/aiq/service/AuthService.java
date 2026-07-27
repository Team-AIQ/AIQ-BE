package cmc.aiq.aiq.service;

import cmc.aiq.aiq.dto.*;
import jakarta.mail.MessagingException;

public interface AuthService {
    void signUp(SignUpRequestDTO request);
    TokenResponseDTO login(LoginRequestDTO loginrequestDTO);
    TokenResponseDTO refresh(String refreshToken);
    void sendResetCode(String email) throws MessagingException;
    String verifyResetCode(String email, String code);
    void resetPassword(String resetToken, String newPassword);
    TokenResponseDTO loginAsGuest();
    void withdrawUser(Long userId);
    void changePassword(Long userId, ChangePasswordRequestDTO request);
    TokenResponseDTO appleLogin(AppleLoginRequestDTO request);
}
