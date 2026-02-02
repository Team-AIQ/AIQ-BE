package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.ApiResponse;
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
    @Operation(summary = "회원가입")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequestDTO request) {
        authService.signUp(request);
        // 새로운 자원이 생성되었으므로 201 Created 사용
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "회원가입 성공", null));
    }

    @PostMapping("/login")
    @Operation(summary = "이메일 로그인")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> login(@RequestBody LoginRequestDTO request) {
        TokenResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "로그인 성공", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> refresh(@RequestHeader("Authorization-Refresh") String refreshToken) {
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        TokenResponseDTO response = authService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "토큰 재발급 성공", response));
    }

    @PostMapping("/email-request")
    @Operation(summary = "매직링크 요청")
    public ResponseEntity<ApiResponse<Void>> requestMagicLink(@RequestParam String email) throws MessagingException {
        mailService.sendMagicLink(email);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "인증 이메일이 발송되었습니다.", null));
    }

    @GetMapping("/verify-link")
    @Operation(summary = "매직링크 검증")
    public ResponseEntity<ApiResponse<String>> verifyMagicLink(@RequestParam String token) {
        String email = mailService.verifyToken(token);
        if (email != null) {
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "인증 성공", "인증된 이메일: " + email));
        } else {
            // 실패 시에도 규격을 맞춰서 401 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.success(HttpStatus.UNAUTHORIZED, "만료되었거나 유효하지 않은 토큰입니다.", null));
        }
    }

    @PostMapping("/password/code-request")
    @Operation(summary = "비밀번호 재설정 코드 요청")
    public ResponseEntity<ApiResponse<Void>> requestResetCode(@RequestParam String email) throws MessagingException {
        authService.sendResetCode(email);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "인증 코드가 발송되었습니다.", null));
    }

    @PostMapping("/password/verify")
    @Operation(summary = "비밀번호 코드 검증")
    public ResponseEntity<ApiResponse<String>> verifyCode(@RequestParam String email, @RequestParam String code) {
        String resetToken = authService.verifyResetCode(email, code);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "코드 검증 완료", resetToken));
    }

    @PatchMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestParam String resetToken, @RequestParam String newPassword) {
        authService.resetPassword(resetToken, newPassword);
        // 수정 완료 후 200 OK 또는 204 No Content
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "비밀번호 재설정 완료", null));
    }
}
