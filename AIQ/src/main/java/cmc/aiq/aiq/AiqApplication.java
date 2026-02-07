package cmc.aiq.aiq;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableJpaAuditing
@PropertySource("classpath:application-secret.properties")
public class AiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiqApplication.class, args);
    }
    @PostConstruct
    public void setupSecurityContext() {
        // ⭐️ 이 설정이 비동기 스레드 인증 유실을 막는 가장 확실한 방법입니다.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
