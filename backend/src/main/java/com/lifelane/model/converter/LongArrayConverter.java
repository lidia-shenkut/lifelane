package com.lifelane.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that maps a Java {@code Long[]} to a PostgreSQL BIGINT[] literal
 * (e.g., {@code {123456,789012}}) and back.
 */
@Converter
public class LongArrayConverter implements AttributeConverter<Long[], String> {

    @Override
    public String convertToDatabaseColumn(Long[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(attribute[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Long[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("{}")) {
            return new Long[0];
        }
        // Strip surrounding braces
        String inner = dbData.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);

        if (inner.isBlank()) {
            return new Long[0];
        }

        String[] parts = inner.split(",");
        Long[] result = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Long.parseLong(parts[i].trim());
        }
        return result;
    }
}
