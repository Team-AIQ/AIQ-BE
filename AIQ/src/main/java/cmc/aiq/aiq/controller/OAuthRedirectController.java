package cmc.aiq.aiq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OAuth Redirect", description = "소셜 로그인 후 앱으로 리다이렉션을 도와주는 API")
@RestController
@RequestMapping("/oauth")
@Log4j2
public class OAuthRedirectController {

    @GetMapping("/app/redirect")
    @Operation(summary = "앱 리다이렉션 페이지", description = "소셜 로그인 성공 후 토큰을 받아 앱의 커스텀 스킴으로 리다이렉트시키는 HTML 페이지를 반환합니다.")
    public ResponseEntity<String> redirectToApp(
            @RequestParam("accessToken") String accessToken,
            @RequestParam("refreshToken") String refreshToken
    ) {
        log.info("앱 리다이렉션 페이지 진입. accessToken 길이: {}", accessToken.length());

        String deepLinkUrl = String.format("aiq://oauth/callback?accessToken=%s&refreshToken=%s", accessToken, refreshToken);
        String html = buildRedirectHtml(deepLinkUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/html; charset=UTF-8");

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    private String buildRedirectHtml(String deepLinkUrl) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>AIQ 로그인 중...</title>"
                + "<script>"
                + "window.onload = function() {"
                + "  try {"
                + "    console.log('Redirecting to: " + deepLinkUrl + "');"
                + "    window.location.href = '" + deepLinkUrl + "';"
                + "  } catch (e) {"
                + "    document.getElementById('message').innerText = '앱을 열 수 없습니다. AIQ 앱이 설치되어 있는지 확인해주세요.';"
                + "    console.error('Failed to redirect:', e);"
                + "  }"
                + "};"
                + "</script>"
                + "</head>"
                + "<body>"
                + "<p id=\"message\">AIQ 앱으로 돌아가는 중입니다...</p>"
                + "</body>"
                + "</html>";
    }
}
