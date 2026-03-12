package cmc.aiq.aiq.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AI-Thread-");
        executor.initialize();

        // ⭐️ 이 녀석이 핵심입니다! 기존 스레드의 보안 컨텍스트를 비동기 스레드로 복사해줍니다.
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
