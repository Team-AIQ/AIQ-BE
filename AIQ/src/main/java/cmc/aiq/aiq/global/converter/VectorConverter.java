package cmc.aiq.aiq.global.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter
public class VectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        // float[]을 "[1.0, 2.0, ...]" 형태의 문자열로 변환합니다.
        // PostgreSQL은 이 문자열을 받으면 자동으로 vector 타입으로 변환해 저장합니다.
        return Arrays.toString(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // DB에서 가져온 "[1.0, 2.0]" 문자열을 다시 float[]로 복구합니다.
        String[] parts = dbData.substring(1, dbData.length() - 1).split(",");
        float[] res = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            res[i] = Float.parseFloat(parts[i].trim());
        }
        return res;
    }
}
