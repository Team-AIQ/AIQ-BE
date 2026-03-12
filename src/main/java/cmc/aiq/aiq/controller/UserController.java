package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.UserResponseDTO;
import cmc.aiq.aiq.global.security.CustomUserDetails;
import cmc.aiq.aiq.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 정보 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보(닉네임, 크레딧 등)를 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponseDTO myInfo = userService.getMyInfo(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "내 정보 조회 성공", myInfo));
    }
}
