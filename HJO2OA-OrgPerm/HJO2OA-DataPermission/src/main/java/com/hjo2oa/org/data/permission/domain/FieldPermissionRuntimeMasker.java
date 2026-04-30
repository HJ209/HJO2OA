package com.hjo2oa.org.data.permission.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class FieldPermissionRuntimeMasker {

    public Map<String, Object> apply(FieldPermissionDecisionView decision, Map<String, Object> row) {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(row, "row must not be null");
        Map<String, Object> masked = new LinkedHashMap<>(row);
        for (String field : decision.hiddenFields()) {
            masked.remove(field);
        }
        for (String field : decision.desensitizedFields()) {
            if (masked.containsKey(field)) {
                masked.put(field, mask(masked.get(field)));
            }
        }
        return masked;
    }

    private String mask(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() <= 2) {
            return "**";
        }
        if (text.length() <= 6) {
            return text.charAt(0) + "***" + text.charAt(text.length() - 1);
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 2);
    }
}
