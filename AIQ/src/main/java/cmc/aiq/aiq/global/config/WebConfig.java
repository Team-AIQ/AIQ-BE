package cmc.aiq.aiq.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000",
                        "http://192.168.219.101:3000",
                        "https://*.ngrok-free.app")
                .allowedMethods("GET", "POST","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
