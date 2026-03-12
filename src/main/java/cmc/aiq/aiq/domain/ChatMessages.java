package cmc.aiq.aiq.domain;

import cmc.aiq.aiq.domain.ENUM.SenderType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessages {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private Queries queries; // 어떤 분석(보고서)에서 이어진 채팅인지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Models model; // 대화 중인 AI 모델 (GPT, Gemini 등)

    @Enumerated(EnumType.STRING)
    private SenderType senderType; // USER, AI

    @Column(columnDefinition = "TEXT")
    private String content; // 메시지 내용

    // 💡 추가 추천 필드
    private Integer roundCount; // 현재 대화 회차 (1~4)

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String suggestedQuestions; // AI가 생성한 '다음 추천 질문 3개' (JSON 형식 저장)

    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public ChatMessages(Queries queries, Models model, SenderType senderType, String content, Integer roundCount, String suggestedQuestions) {
        this.queries = queries;
        this.model = model;
        this.senderType = senderType;
        this.content = content;
        this.roundCount = roundCount;
        this.suggestedQuestions = suggestedQuestions;
        this.createdAt = LocalDateTime.now(); // 생성 시점 기록
    }
}
