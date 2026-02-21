package cmc.aiq.aiq.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CreditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private Long amount; // 지급 또는 차감된 크레딧 양

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter; // 트랜잭션 후 잔액

    @Column(nullable = false)
    private String reason; // 지급 또는 차감 사유

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CreditLog(Users user, Long amount, Long balanceAfter, String reason) {
        this.user = user;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
    }
}
