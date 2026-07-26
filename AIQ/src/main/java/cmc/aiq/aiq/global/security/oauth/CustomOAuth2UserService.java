package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // [수정] 각 소셜 로그인 방식에 따라 사용할 변수들을 미리 선언합니다.
        String email;
        String nickname;
        String providerId;
        
        // [수정] registrationId에 따라 분기 처리하여 올바른 정보를 추출합니다.
        if (registrationId.equalsIgnoreCase("apple")) {
            // Apple 로그인 처리
            String idToken = userRequest.getAdditionalParameters().get("id_token").toString();
            Map<String, Object> attributes = parseAppleIdToken(idToken);
            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            // Apple은 닉네임을 제공하지 않으므로, 기본값을 설정합니다.
            nickname = "Apple 사용자"; 
        } else {
            // Google, Naver, Kakao 등 나머지 소셜 로그인 처리
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();
            email = extractEmail(attributes, registrationId);
            nickname = extractNickname(attributes, registrationId);
            providerId = extractProviderId(attributes, registrationId);
        }

        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        // [수정] 분기 처리 후, 공통 로직으로 사용자를 찾거나 생성합니다.
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

        // [복원] Spring Security의 기본 OAuth2User를 반환합니다.
        // SuccessHandler에서 email과 provider로 다시 조회하므로, 여기서는 기본 객체를 반환해도 안전합니다.
        return super.loadUser(userRequest);
    }

    private Map<String, Object> parseAppleIdToken(String idToken) {
        try {
            String[] chunks = idToken.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Apple id_token을 파싱하는 중 오류 발생", e);
        }
    }

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("email");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("email");
            case "kakao" -> (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        };
    }

    private String extractNickname(Map<String, Object> attributes, String registrationId) {
        return switch (registrationId) {
            case "google" -> (String) attributes.get("name");
            case "naver" -> (String) ((Map<String, Object>) attributes.get("response")).get("nickname");
            case "kakao" -> (String) ((Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile")).get("nickname");
            default -> "사용자"; // 예외 케이스에 대한 기본값
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
