package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(attributes, registrationId);
        String nickname = extractNickname(attributes, registrationId);
        String providerId = extractProviderId(attributes, registrationId);
        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        // [수정] .orElseGet 안에서 save를 호출하여 트랜잭션 내에서 완료되도록 보장
        Users user = usersRepository.findByProviderAndProviderId(authProvider, providerId)
                .map(existingUser -> {
                    // 기존 유저 정보 업데이트 (필요 시)
                    return existingUser.updateSocialInfo(nickname);
                })
                .orElseGet(() -> {
                    // 신규 유저 생성
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

        // [수정] 우리 시스템의 Users 객체를 포함하는 CustomOAuth2User를 반환
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                userNameAttributeName,
                user
        );
    }

    // ... (extractEmail, extractNickname, extractProviderId 메소드는 동일)
}
