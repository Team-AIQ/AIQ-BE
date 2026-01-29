package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.service.AuthService;
import cmc.aiq.aiq.service.Mail.MailService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    private final AuthService authService;
    private final MailService mailService;

    @PostMapping("/signup")
    public String signUp(@RequestBody SignUpRequestDTO request){
        authService.signUp(request);
        return "회원가입 성공!";
    }

    @PostMapping("/login")
    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 Access/Refresh 토큰을 발급합니다.")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody LoginRequestDTO request) {
        TokenResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh 토큰을 사용하여 새로운 Access/Refresh 토큰을 발급합니다.")
    public ResponseEntity<TokenResponseDTO> refresh(@RequestHeader("Authorization-Refresh") String refreshToken) {
        // 'Bearer ' 접두사가 붙어 올 경우를 대비해 잘라줍니다.
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;

        TokenResponseDTO response = authService.refresh(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email-request")
    public ResponseEntity<String> requestMagicLink(@RequestParam String email) throws MessagingException {
        mailService.sendMagicLink(email);
        return ResponseEntity.ok("인증 이메일이 발송되었습니다.");
    }

    // 2. 매직 링크 클릭 시 처리 (GET 요청)
    @GetMapping("/verify-link")
    public ResponseEntity<String> verifyMagicLink(@RequestParam String token) {
        String email = mailService.verifyToken(token);

        if (email != null) {
            // 여기서 해당 이메일을 '인증됨' 상태로 Redis에 잠깐 저장하거나,
            // 바로 회원가입 프로세스로 리다이렉트 시킵니다.
            return ResponseEntity.ok("인증 성공! 이메일: " + email);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("만료되었거나 유효하지 않은 토큰입니다.");
        }
    }

    @PostMapping("/password/code-request")
    public ResponseEntity<Void> requestResetCode(@RequestParam String email) throws MessagingException {
        authService.sendResetCode(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email, @RequestParam String code) {
        return ResponseEntity.ok(authService.verifyResetCode(email, code));
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestParam String resetToken, @RequestParam String newPassword) {
        authService.resetPassword(resetToken, newPassword);
        return ResponseEntity.ok().build();
    }
}
