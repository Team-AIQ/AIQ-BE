package cmc.aiq.aiq.service.Mail;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendMagicLink(String email , String origin) throws MessagingException;
    String verifyToken(String token);
    void sendVerificationCode(String email, String code) throws MessagingException;
}
