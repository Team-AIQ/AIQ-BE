package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(attributes, registrationId);
        String nickname = extractNickname(attributes, registrationId);
        String providerId = extractProviderId(attributes, registrationId);
        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        // [복원] DB에서 사용자를 찾거나, 없으면 새로 생성하여 저장하는 로직은 그대로 유지
        usersRepository.findByProviderAndProviderId(authProvider, providerId)
                .map(existingUser -> existingUser.updateSocialInfo(nickname))
                .orElseGet(() -> {
                    Users newUser = Users.builder()
                            .email(email)
                            .nickname(nickname)
                            .provider(authProvider)
                            .providerId(providerId)
                            .initialLoginAt(LocalDateTime.now())
                            .currentCredits(20L)
                            .build();
                    return usersRepository.save(newUser);
                });

        // [복원] CustomOAuth2User 대신, Spring Security의 기본 OAuth2User를 그대로 반환
        return oAuth2User;
    }

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("email");
            case "naver" -> (Map<String, Object>) attributes.get("response") != null
                    ? (String) ((Map<String, Object>) attributes.get("response")).get("email") : null;
            case "kakao" -> (Map<String, Object>) attributes.get("kakao_account") != null
                    ? (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email") : null;
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }

    private String extractNickname(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("name");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("nickname");
            case "kakao" -> (String) ((Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile")).get("nickname");
            default -> (String) attributes.get("nickname");
        };
    }

    private String extractProviderId(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("sub");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("id");
            case "kakao" -> String.valueOf(attributes.get("id"));
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }
}
