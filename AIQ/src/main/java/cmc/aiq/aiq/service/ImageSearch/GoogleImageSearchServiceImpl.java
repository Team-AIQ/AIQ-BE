package cmc.aiq.aiq.service.ImageSearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

    @Override
    public String getProductImageUrl(String productName) {
        // UriComponentsBuilder를 사용하여 URL을 안전하게 구성하고 인코딩합니다.
        String url = UriComponentsBuilder.fromHttpUrl("https://openapi.naver.com/v1/search/shop.json")
                .queryParam("query", productName)
                .queryParam("display", 1)
                .queryParam("sort", "sim")
                .build() // 먼저 구조를 만들고
                .encode() // 그 다음 인코딩을 명시적으로 수행
                .toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = responseEntity.getBody();
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                if (items != null && !items.isEmpty()) {
                    return (String) items.get(0).get("image");
                }
            }

        } catch (Exception e) {
            log.error("Naver 쇼핑 이미지 검색 중 오류 발생 - 제품명: {}", productName, e);
            return "https://placehold.co/600x400?text=No+Image";
        }

        return "https://placehold.co/600x400?text=No+Image";
    }
}
