package cmc.aiq.aiq.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "prompt_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false)
    private String templateKey; // 예: "AI_RECOMMEND_SYSTEM", "REPORT_AGENT_SYSTEM"

    private String version;

    @Column(columnDefinition = "TEXT")
    private String content; // 프롬프트 내용 ({{variable}} 포함 가능)

    @Column(name = "is_active")
    private boolean isActive = true;

    private LocalDateTime deletedAt;

    // 비즈니스 로직: 변수 치환 메서드
    public String format(Map<String, String> variables) {
        String result = this.content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
