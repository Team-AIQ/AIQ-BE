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

import java.net.URI;

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
    public ResponseEntity<ApiResponse<Void>> requestMagicLink(@RequestParam String email, @RequestParam String origin) throws MessagingException {
        mailService.sendMagicLink(email , origin);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "인증 이메일이 발송되었습니다.", null));
    }

    @GetMapping("/verify-link")
    @Operation(summary = "매직링크 검증")
    public ResponseEntity<ApiResponse<String>> verifyMagicLink(@RequestParam String token) {
        String verificationData = mailService.verifyToken(token);

        if (verificationData != null) {
            String[] parts = verificationData.split(":");
            String email = parts[0];
            String origin = parts[1];

            String redirectUrl;
            if ("app".equalsIgnoreCase(origin)) {
                // 앱: 설정해둔 커스텀 스킴(aiq://)을 통한 딥링크 리다이렉트
                redirectUrl = "http://192.168.219.101:8080://signup-success?email=" + email;
            } else {
                // 웹: Next.js의 가입 완료 혹은 추가 정보 입력 페이지
                redirectUrl = "http://localhost:3000/signup?verified=1&email=" + email;
            }

            return ResponseEntity.status(HttpStatus.FOUND) // 302 Redirect
                    .location(URI.create(redirectUrl))
                    .build();
        } else {
            // 인증 실패 시 에러 안내 페이지(웹)로 이동시키거나 에러용 딥링크 발송
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/auth/error?reason=expired"))
                    .build();
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
