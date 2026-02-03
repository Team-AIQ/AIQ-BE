package cmc.aiq.aiq.domain;

import cmc.aiq.aiq.domain.ENUM.ResponseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "ai_responses", indexes = {
        @Index(name = "idx_query_id", columnList = "query_id"),
        @Index(name = "idx_model_id", columnList = "model_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private Queries queries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Models model;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private ResponseStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // ⭐️ [추가] AI 요청 시작 시 사용하는 생성자
    @Builder
    public AiResponse(Queries queries, Models model) {
        this.queries = queries;
        this.model = model;
        this.status = ResponseStatus.PENDING; // 생성과 동시에 대기 상태로!
        this.metadata = new HashMap<>();      // NPE 방지
    }

    // 성공 시 호출할 비즈니스 메서드
    public void complete(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
        this.status = ResponseStatus.COMPLETED;
    }

    // 실패 시 호출할 비즈니스 메서드
    public void fail(String errorMessage) {
        this.status = ResponseStatus.FAILED;
        this.metadata.put("error_message", errorMessage);
    }
}
