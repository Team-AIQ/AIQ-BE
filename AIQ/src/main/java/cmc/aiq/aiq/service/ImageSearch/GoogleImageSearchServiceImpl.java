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
        String url = UriComponentsBuilder.fromHttpUrl("https://openapi.naver.com/v1/search/shop.json")
                .queryParam("query", productName)
                .queryParam("display", 1) // 가장 정확한 결과 1개만 받음
                .queryParam("sort", "sim") // 정확도순으로 정렬
                .build(true) // queryParam에 있는 한글 등이 깨지지 않도록 인코딩
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
                    // 네이버 쇼핑 API는 'image' 필드에 이미지 URL이 담겨 있습니다.
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
