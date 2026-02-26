package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.ChangePasswordRequestDTO;
import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.global.security.CustomUserDetails;
import cmc.aiq.aiq.service.AuthService;
import cmc.aiq.aiq.service.Mail.MailService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> guestLogin() {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "비회원 로그인 성공", authService.loginAsGuest()));
    }

    @GetMapping("/verify-link")
    @Operation(summary = "매직링크 검증")
    public ResponseEntity<Void> verifyMagicLink(@RequestParam String token) {
        String verificationData = mailService.verifyToken(token);

        if (verificationData != null) {
            String[] parts = verificationData.split(":");
            String email = parts[0];
            String origin = parts[1];

            String redirectUrl;
            if ("app".equalsIgnoreCase(origin)) {
                redirectUrl = "aiq://signup-success?email=" + email; // 실제 앱 커스텀 스킴으로 수정
            } else {
                redirectUrl = "https://www.aiq.ai.kr/signup?verified=1&email=" + email;
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("https://www.aiq.ai.kr/auth/error?reason=expired"))
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
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "비밀번호 재설정 완료", null));
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자의 계정을 탈퇴 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.withdrawUser(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "회원 탈퇴가 성공적으로 처리되었습니다.", null));
    }

    @PatchMapping("/password/change")
    @Operation(summary = "로그인 후 비밀번호 변경", description = "현재 로그인된 사용자가 자신의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequestDTO request
    ) {
        authService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "비밀번호가 성공적으로 변경되었습니다.", null));
    }
}
