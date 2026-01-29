package cmc.aiq.aiq.domain;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
@Entity
@Table(name = "queries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Queries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user; // 유저와의 양방향 관계 (ManyToOne)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Enumerated(EnumType.STRING) // Enum 사용 권장 (PENDING, COMPLETED, FAILED)
    private QueryStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Queries(Users user, String question) {
        this.user = user;
        this.question = question;
        this.status = QueryStatus.PENDING; // 생성 시 기본값
    }

    // 상태 변경을 위한 메서드
    public void complete() { this.status = QueryStatus.COMPLETED; }
    public void fail() { this.status = QueryStatus.FAILED; }
}
