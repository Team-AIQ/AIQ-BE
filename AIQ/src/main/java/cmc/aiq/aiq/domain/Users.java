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
    private String nickname; // 서비스 내 활동 닉네임

    @Column(name = "current_credits", nullable = false)
    private Long currentCredits = 0L; // 기본 크레딧 설정

    // --- 소셜 로그인 연동 필드 ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider; // EMAIL, KAKAO, NAVER, GOOGLE

    @Column(name = "provider_id", nullable = true) // 이메일 가입자는 null이 들어가므로 nullable 허용
    private String providerId;

    // --- 시간 기록 필드 ---
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 계정 삭제 시 시각 기록 (Soft Delete용)

    @Column(name = "refresh_token")
    private String refreshToken;

    @Builder
    public Users(String email, String password, String nickname,
                 AuthProvider provider, String providerId, Long currentCredits) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.currentCredits = (currentCredits != null) ? currentCredits : 0L;
    }
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
