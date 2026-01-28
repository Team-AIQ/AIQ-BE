package cmc.aiq.aiq.service.Mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void sendMagicLink(String email) throws MessagingException {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(token, email, Duration.ofMinutes(5));

        String verificationLink = "http://localhost:8080/api/auth/verify-link?token=" + token;

        MimeMessage message = mailSender.createMimeMessage();
        // true는 multipart 메시지를 사용하겠다는 의미 (이미지 등 첨부 가능)
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("[AIQ] 본인 인증 확인 메일입니다.");

        // 위에 작성한 HTML 템플릿을 문자열로 넣습니다.
        String htmlContent = getEmailHtml(verificationLink);

        // true를 설정해야 HTML로 인식합니다.
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    private String getEmailHtml(String verificationLink) {
        // 실제로는 별도의 템플릿 엔진(Thymeleaf 등)을 쓰거나
        // 외부 파일에서 읽어오는 것이 깔끔하지만, 우선 String으로 구현해 드릴게요.
        return "<div style=\"font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; width: 100%; max-width: 600px; margin: 0 auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden;\">\n" +
                "    <div style=\"background-color: #f8f9fa; padding: 30px; text-align: center;\">\n" +
                "        <img src=\"https://your-domain.com/logo.png\" alt=\"AIQ Logo\" style=\"width: 120px; margin-bottom: 10px;\">\n" +
                "        <h2 style=\"color: #333; margin: 0;\">이메일 인증 안내</h2>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div style=\"padding: 40px; background-color: #ffffff;\">\n" +
                "        <p style=\"font-size: 16px; color: #555; line-height: 1.6;\">\n" +
                "            안녕하세요!<br>\n" +
                "            AI 모델 비교 서비스 <strong>AIQ</strong>를 이용해 주셔서 감사합니다.<br>\n" +
                "            본인 확인을 위해 아래 버튼을 클릭하여 인증을 완료해 주세요.\n" +
                "        </p>\n" +
                "        \n" +
                "        <div style=\"text-align: center; margin: 40px 0;\">\n" +
                "            <a href=\"${verificationLink}\" style=\"background-color: #007bff; color: white; padding: 15px 35px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 18px; display: inline-block;\">\n" +
                "                AIQ 시작하기\n" +
                "            </a>\n" +
                "        </div>\n" +
                "        \n" +
                "        <p style=\"font-size: 13px; color: #888; text-align: center;\">\n" +
                "            이 버튼은 5분 동안 유효합니다.<br>\n" +
                "            본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.\n" +
                "        </p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div style=\"background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #aaa;\">\n" +
                "        © 2026 AIQ Project. All rights reserved.<br>\n" +
                "        본 메일은 발신 전용입니다.\n" +
                "    </div>\n" +
                "</div>";
    }
    @Override
    public String verifyToken(String token) {
        // Redis에서 토큰으로 이메일 조회
        String email = redisTemplate.opsForValue().get(token);

        if (email != null) {
            // 인증 성공 시 토큰 삭제 (1회용)
            redisTemplate.delete(token);
            return email;
        }
        return null;
    }
}
