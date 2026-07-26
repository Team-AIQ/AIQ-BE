package cmc.aiq.aiq.config;

import cmc.aiq.aiq.global.security.oauth.AppleJwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

    private final AppleJwtUtils appleJwtUtils;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        ClientRegistration appleRegistration = clientRegistrationRepository.findByRegistrationId("apple");

        ClientRegistration customAppleRegistration = ClientRegistration.withClientRegistration(appleRegistration)
                .clientSecret(appleJwtUtils.createClientSecret())
                .build();

        return new InMemoryClientRegistrationRepository(
                customAppleRegistration,
                clientRegistrationRepository.findByRegistrationId("google"),
                clientRegistrationRepository.findByRegistrationId("naver"),
                clientRegistrationRepository.findByRegistrationId("kakao")
        );
    }
}
