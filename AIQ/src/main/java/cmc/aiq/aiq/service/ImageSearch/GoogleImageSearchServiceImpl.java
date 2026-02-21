package cmc.aiq.aiq.service.ImageSearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Value("${serper.api.key}")
    private String serperApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProductImageUrl(String productName) {
        String url = "https://google.serper.dev/images";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", serperApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Serper는 POST 요청에 JSON 본문을 사용합니다.
            // 따옴표 문제를 피하기 위해 간단한 Map과 ObjectMapper를 사용하는 것이 더 안전하지만,
            // 간단한 구현을 위해 문자열 조합을 사용합니다.
            String requestBody = "{\"q\":\"" + productName.replace("\"", "\\\"") + "\"}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> body = responseEntity.getBody();
            if (body != null && body.containsKey("images")) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) body.get("images");
                if (images != null && !images.isEmpty()) {
                    // 'imageUrl' 필드에 이미지 주소가 담겨 있습니다.
                    return (String) images.get(0).get("imageUrl");
                }
            }

        } catch (Exception e) {
            log.error("Serper 이미지 검색 중 오류 발생 - 제품명: {}", productName, e);
            return "https://placehold.co/600x400?text=No+Image";
        }

        return "https://placehold.co/600x400?text=No+Image";
    }
}
