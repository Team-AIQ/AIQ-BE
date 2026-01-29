package cmc.aiq.aiq.dto.Quration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryAttributesDTO implements Serializable {
    private String attribute_key;
    private String display_label;
    private String question_text;
    private List<String> options;
}
