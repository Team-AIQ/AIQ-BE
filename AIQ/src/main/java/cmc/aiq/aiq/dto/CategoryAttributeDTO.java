package cmc.aiq.aiq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryAttributeDTO implements Serializable {
    private String attribute_key;
    private String display_label;
    private String question_text;
    private List<String> options;
}
