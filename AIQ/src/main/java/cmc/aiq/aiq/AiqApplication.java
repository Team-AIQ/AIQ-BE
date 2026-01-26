package cmc.aiq.aiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiqApplication.class, args);
    }

}
