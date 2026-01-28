package cmc.aiq.aiq.service.Mail;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendMagicLink(String email) throws MessagingException;
    String verifyToken(String token);
}
