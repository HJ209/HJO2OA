package com.hjo2oa.data.report.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ReportCaliberDefinition(
        String sourceProviderKey,
        String subjectCode,
        String defaultTimeField,
        String organizationField,
        String dataServiceCode,
        Map<String, String> baseFilters,
        List<String> triggerEventTypes,
        String description
) {

    public ReportCaliberDefinition {
        sourceProviderKey = requireText(sourceProviderKey, "sourceProviderKey");
        subjectCode = requireText(subjectCode, "subjectCode");
        defaultTimeField = blankToNull(defaultTimeField);
        organizationField = blankToNull(organizationField);
        dataServiceCode = blankToNull(dataServiceCode);
        baseFilters = baseFilters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(baseFilters));
        triggerEventTypes = triggerEventTypes == null ? List.of() : List.copyOf(triggerEventTypes);
        description = blankToNull(description);
    }

    public boolean matchesEvent(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return false;
        }
        return triggerEventTypes.stream().anyMatch(pattern -> matchesPattern(pattern, eventType));
    }

    private static boolean matchesPattern(String pattern, String eventType) {
        String normalizedPattern = blankToNull(pattern);
        if (normalizedPattern == null) {
            return false;
        }
        if (normalizedPattern.endsWith("*")) {
            return eventType.startsWith(normalizedPattern.substring(0, normalizedPattern.length() - 1));
        }
        return Objects.equals(normalizedPattern, eventType);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
