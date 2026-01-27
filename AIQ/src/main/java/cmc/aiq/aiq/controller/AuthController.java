package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.LoginRequestDTO;
import cmc.aiq.aiq.dto.SignUpRequestDTO;
import cmc.aiq.aiq.dto.TokenResponseDTO;
import cmc.aiq.aiq.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public String signUp(@RequestBody SignUpRequestDTO request){
        authService.signUp(request.getEmail(), request.getPassword(), request.getNickname());
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
}
