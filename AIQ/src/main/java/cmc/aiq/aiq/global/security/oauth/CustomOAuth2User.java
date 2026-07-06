package cmc.aiq.aiq.global.security.oauth;

import cmc.aiq.aiq.domain.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Security가 이해하는 OAuth2User이면서,
 * 동시에 우리 시스템의 Users 엔티티를 직접 담을 수 있는 커스텀 Principal 클래스.
 * 이 클래스 덕분에 SuccessHandler에서 DB를 다시 조회할 필요가 없어진다.
 */
@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final Users user;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes, String nameAttributeKey,
                            Users user) {
        super(authorities, attributes, nameAttributeKey);
        this.user = user;
    }
}
