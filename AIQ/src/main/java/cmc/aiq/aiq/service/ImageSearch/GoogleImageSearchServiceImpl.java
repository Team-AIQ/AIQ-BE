package cmc.aiq.aiq.service.ImageSearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class GoogleImageSearchServiceImpl implements GoogleImageSearchService {

    @Value("${naver.client.id}")
    private String naverClientId;

    @Value("${naver.client.secret}")
    private String naverClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String NAVER_API_URL = "https://openapi.naver.com/v1/search/shop.json?query={query}&display=1&sort=sim";

    @Override
    public String getProductImageUrl(String productName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // ★ 기존에 있던 NAVER_API_URL 변수를 쓰지 않고 여기서 직접 깨끗한 URL을 조립합니다.
            URI uri = UriComponentsBuilder.fromHttpUrl("https://openapi.naver.com/v1/search/shop.json")
                    .queryParam("query", productName)
                    .queryParam("display", 1)
                    .queryParam("sort", "sim")
                    .encode() // 한글 및 특수문자 안전하게 자동 인코딩
                    .build()
                    .toUri();

            // ★ 배포 후 로그에서 실제로 어떤 주소로 요청이 갔는지 눈으로 직접 확인할 수 있도록 로그 추가!
            log.info("네이버 쇼핑 API 실제 요청 URL: {}", uri.toString());

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = responseEntity.getBody();
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                if (items != null && !items.isEmpty()) {
                    String imageUrl = (String) items.get(0).get("image");
                    log.info("네이버 이미지 검색 성공: {} -> {}", productName, imageUrl);
                    return imageUrl;
                }
            }
            log.warn("네이버 이미지 검색 결과 없음: {}", productName);
            return "https://placehold.co/600x400?text=No+Image";

        } catch (Exception e) {
            // e.getMessage()를 통해 불필요하게 긴 스택 트레이스 대신 핵심 에러 메시지만 깔끔하게 출력
            log.error("네이버 쇼핑 이미지 검색 중 오류 발생 - 제품명: {}, 에러: {}", productName, e.getMessage());
            return "https://placehold.co/600x400?text=No+Image";
        }
    }
}
