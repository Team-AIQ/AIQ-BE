package cmc.aiq.aiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@PropertySource("classpath:application-secrets.properties")
public class AiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiqApplication.class, args);
    }

}
