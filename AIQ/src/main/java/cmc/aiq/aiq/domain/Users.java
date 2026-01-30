package cmc.aiq.aiq.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)

public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "current_credits", nullable = false)
    private Long currentCredits = 0L;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.ROLE_USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // EMAIL, KAKAO, NAVER, GOOGLE

    @Column(name = "provider_id", nullable = true)
    private String providerId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "initial_login_at")
    private LocalDateTime initialLoginAt;

    @Builder
    public Users(String email, String password, String nickname,
                 AuthProvider provider, String providerId, Long currentCredits , LocalDateTime initialLoginAt) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.initialLoginAt = initialLoginAt;
        this.currentCredits = (currentCredits != null) ? currentCredits : 0L;
    }
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public Users updateSocialInfo(String nickname){
        this.nickname = nickname;
        return this;
    }
    public void updateInitialLoginAt(LocalDateTime time){
        this.initialLoginAt = time;
    }
    public void updatePassword(String password){
        this.password = password;
    }


}
