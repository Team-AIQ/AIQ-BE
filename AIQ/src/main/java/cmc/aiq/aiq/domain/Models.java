package cmc.aiq.aiq.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "models")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Models {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name; // ex) GPT, Gemini, Perplexity

    @Column(name = "version")
    private String version; // ex) gpt-4o-flash

    @Builder
    public Models(String name, String version) {
        this.name = name;
        this.version = version;
    }
}
