package cmc.aiq.aiq.domain;

import cmc.aiq.aiq.dto.Quration.CategoryAttributesDTO;
import cmc.aiq.aiq.global.converter.VectorConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "category_attributes")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryAttributes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", unique = true, nullable = false)
    private String categoryName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // pgvector 타입 매핑 (Hibernate 6 기준)
    @Column(columnDefinition = "vector(1536)")
    @Convert(converter = VectorConverter.class)
    private float[] embedding;

    // JSONB 타입을 List<DTO>로 자동 매핑
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private List<CategoryAttributesDTO> attributes;

    @Column(name = "is_ai_generated")
    private boolean isAiGenerated = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public CategoryAttributes(String categoryName, String displayName, float[] embedding,
                              List<CategoryAttributesDTO> attributes, boolean isAiGenerated) {
        this.categoryName = categoryName;
        this.displayName = displayName;
        this.embedding = embedding;
        this.attributes = attributes;
        this.isAiGenerated = isAiGenerated;
        this.createdAt = LocalDateTime.now(); // 생성 시점 기록
    }
}
