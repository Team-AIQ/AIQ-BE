package cmc.aiq.aiq.global.config;

import cmc.aiq.aiq.domain.Models;
import cmc.aiq.aiq.repository.ModelsRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel; // 추가된 의존성에서 가져옴
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class LangChainConfig {
    private final ModelsRepository modelsRepository;

    @Bean(name = "gptModel")
    @Primary
    public ChatLanguageModel gptModel(@Value("${openai.api.key}") String apiKey) {
        // DB에서 'GPT'라는 이름의 모델 정보를 찾습니다.
        String version = modelsRepository.findByName("GPT")
                .map(Models::getVersion)
                .orElse("gpt-4o"); // 혹시 DB에 없으면 기본값 사용

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(version) // DB에서 가져온 gpt-4o 적용
                .build();
    }

    // 2. Gemini (Google AI)
    @Bean(name = "geminiModel")
    public ChatLanguageModel geminiModel(@Value("${google.api.key}") String apiKey) {
        String version = getModelVersion("Gemini", "gemini-2.5-flash");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(version)
                .build();
    }

    // 3. Perplexity 모델 (sonar)
    // Perplexity는 OpenAI 호환 API를 사용하므로 baseUrl만 바꿔주면 됩니다!
    @Bean(name = "perplexityModel")
    public ChatLanguageModel perplexityModel(@Value("${perplexity.api.key}") String apiKey) {
        String version = getModelVersion("Perplexity", "sonar");
        return OpenAiChatModel.builder()
                .baseUrl("https://api.perplexity.ai") // 퍼플렉시티 주소
                .apiKey(apiKey)
                .modelName(version)
                .build();
    }

    // 4. OpenAI 임베딩 모델
    @Bean
    public EmbeddingModel embeddingModel(@Value("${openai.api.key}") String apiKey) {
        String version = getModelVersion("OpenAI", "text-embedding-3-small");
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(version)
                .build();
    }

    // DB에서 버전 가져오는 공통 메서드
    private String getModelVersion(String name, String defaultVersion) {
        return modelsRepository.findByName(name)
                .map(Models::getVersion)
                .orElse(defaultVersion);
    }
}
