package com.lifelane.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * JPA converter that maps a Java {@code String[]} to a PostgreSQL TEXT[] literal
 * (e.g., {@code {"AED","oxygen"}}) and back.
 */
@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(",");
            // Escape double-quotes inside the value and wrap in quotes
            sb.append("\"").append(attribute[i].replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("{}")) {
            return new String[0];
        }
        // Strip surrounding braces
        String inner = dbData.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);

        if (inner.isBlank()) {
            return new String[0];
        }

        // Simple CSV-aware split that handles quoted elements
        return Arrays.stream(inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(s -> s.trim())
                .map(s -> {
                    if (s.startsWith("\"") && s.endsWith("\"")) {
                        s = s.substring(1, s.length() - 1);
                    }
                    return s.replace("\\\"", "\"").replace("\\\\", "\\");
                })
                .toArray(String[]::new);
    }
}
