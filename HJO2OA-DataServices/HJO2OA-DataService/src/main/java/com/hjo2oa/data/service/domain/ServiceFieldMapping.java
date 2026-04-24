package com.hjo2oa.data.service.domain;

import java.util.Objects;
import java.util.UUID;

public record ServiceFieldMapping(
        UUID mappingId,
        UUID serviceId,
        String sourceField,
        String targetField,
        TransformRule transformRule,
        boolean masked,
        String description,
        int sortOrder
) {

    public ServiceFieldMapping {
        Objects.requireNonNull(mappingId, "mappingId must not be null");
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        sourceField = requireText(sourceField, "sourceField");
        targetField = requireText(targetField, "targetField");
        transformRule = transformRule == null ? TransformRule.none() : transformRule;
        description = normalizeNullableText(description);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    public DataServiceViews.FieldMappingView toView() {
        return new DataServiceViews.FieldMappingView(
                mappingId,
                sourceField,
                targetField,
                transformRule,
                masked,
                description,
                sortOrder
        );
    }

    public record TransformRule(
            TransformType type,
            String expression,
            String formatPattern,
            String constantValue
    ) {

        public TransformRule {
            type = type == null ? TransformType.DIRECT : type;
            expression = normalizeNullableText(expression);
            formatPattern = normalizeNullableText(formatPattern);
            constantValue = normalizeNullableText(constantValue);
            if (type == TransformType.DATE_FORMAT && formatPattern == null) {
                throw new IllegalArgumentException("formatPattern is required for DATE_FORMAT");
            }
            if (type == TransformType.CONSTANT && constantValue == null) {
                throw new IllegalArgumentException("constantValue is required for CONSTANT");
            }
        }

        public static TransformRule none() {
            return new TransformRule(TransformType.DIRECT, null, null, null);
        }
    }

    public enum TransformType {
        DIRECT,
        TRIM,
        UPPERCASE,
        LOWERCASE,
        DATE_FORMAT,
        CONSTANT
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
