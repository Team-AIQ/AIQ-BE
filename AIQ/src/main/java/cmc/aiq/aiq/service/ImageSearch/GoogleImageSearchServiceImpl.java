package cmc.aiq.aiq.service.ImageSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class GoogleImageSearchServiceImpl implements GoogleImageSearchService{
    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.search.cx}")
    private String cx;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getProductImageUrl(String productName) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/customsearch/v1")
                    .queryParam("key", apiKey)
                    .queryParam("cx", cx)
                    .queryParam("q", productName + " 공식 이미지") // 검색 정확도를 높이기 위한 키워드 추가
                    .queryParam("searchType", "image")
                    .queryParam("num", 1)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items != null && !items.isEmpty()) {
                return (String) items.get(0).get("link");
            }
        } catch (Exception e) {
            // 에러 시 기본 이미지 반환
            return "https://placehold.co/600x400?text=No+Image";
        }
        return "https://placehold.co/600x400?text=No+Image";
    }
}
