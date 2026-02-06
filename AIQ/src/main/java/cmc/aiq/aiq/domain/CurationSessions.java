package cmc.aiq.aiq.domain;

import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import cmc.aiq.aiq.dto.Quration.CurationSubmitRequestDTO;
import cmc.aiq.aiq.dto.Quration.CurationUserAnswerDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "curation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurationSessions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저와의 연관관계 (누구의 큐레이션인가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 원본 질문과의 연관관계 (어떤 질문에서 시작되었는가)
    // original_question은 이 query 객체 안에 들어있습니다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false, unique = true)
    private Queries query;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_attributes", nullable = false, unique = true)
    private CategoryAttributes categoryAttributes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "curation_results", columnDefinition = "jsonb")
    private List<CurationUserAnswerDTO> curationResults;

    @Builder
    public CurationSessions(Users user, Queries query, CategoryAttributes categoryAttributes, List<CurationUserAnswerDTO> curationResults) {
        this.user = user;
        this.query = query;
        this.categoryAttributes = categoryAttributes;
        this.curationResults = curationResults;
    }

    public void updateResults(List<CurationSubmitRequestDTO.AnswerItem> newAnswers) {
        if (this.curationResults == null || newAnswers == null) return;

        for (CurationSubmitRequestDTO.AnswerItem answer : newAnswers) {
            // curationResults 리스트를 돌면서 매칭되는 항목 찾기
            this.curationResults.stream()
                    .filter(res -> res.getDisplayLabel().equals(answer.getDisplayLabel()))
                    .findFirst()
                    .ifPresent(res -> {
                        // 사용자가 선택한 답변으로 업데이트
                        res.updateSelectedAnswer(answer.getUserAnswer());
                    });
        }
    }
}
