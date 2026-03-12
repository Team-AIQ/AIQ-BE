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

            // [수정] RestTemplate이 URI 변수({query})를 자동으로 인코딩하도록 변경
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    NAVER_API_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    productName // URI 변수 {query}에 productName 값을 매핑
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
            log.error("네이버 쇼핑 이미지 검색 중 오류 발생 - 제품명: {}", productName, e);
            return "https://placehold.co/600x400?text=No+Image";
        }
    }
}
