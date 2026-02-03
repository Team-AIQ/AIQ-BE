package cmc.aiq.aiq.service.MultiAiService;

import cmc.aiq.aiq.domain.PromptTemplate;
import cmc.aiq.aiq.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptManager {
    private final PromptTemplateRepository repository;

    public String getProcessedPrompt(String key, Map<String, String> variables) {
        PromptTemplate template = repository.findByTemplateKeyAndIsActive(key, true)
                .orElseThrow(() -> new RuntimeException("활성화된 프롬프트 템플릿을 찾을 수 없습니다: " + key));

        return template.format(variables);
    }
}
