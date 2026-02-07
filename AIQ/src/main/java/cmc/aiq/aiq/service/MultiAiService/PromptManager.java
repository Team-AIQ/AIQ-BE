package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.PromptTemplate;
import cmc.aiq.aiq.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class PromptManager {
    private final PromptTemplateRepository repository;


    @Transactional(readOnly = true)
    public String getProcessedPrompt(String key, Map<String, String> variables) {
        log.info("프롬프트 조회 시작 - Key: {}", key);

        // 1. DB에서 해당 키의 활성화된(isActive = true) 최신 템플릿 조회
        PromptTemplate template = repository.findByIsActiveAndTemplateKey(true, key)
                .orElseThrow(() -> new RuntimeException("활성화된 프롬프트 템플릿을 찾을 수 없습니다: " + key));

        // 2. 엔티티 내부의 format 메서드를 호출하여 {{variable}} 치환
        String processedPrompt = template.format(variables);

        log.info("프롬프트 치환 완료 - Key: {}", key);
        return processedPrompt;
    }

}
