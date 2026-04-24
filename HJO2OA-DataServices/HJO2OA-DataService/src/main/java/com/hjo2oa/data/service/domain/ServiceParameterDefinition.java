package com.hjo2oa.data.service.domain;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ServiceParameterDefinition(
        UUID parameterId,
        UUID serviceId,
        String paramCode,
        ParameterType paramType,
        boolean required,
        String defaultValue,
        ValidationRule validationRule,
        boolean enabled,
        String description,
        int sortOrder
) {

    public ServiceParameterDefinition {
        Objects.requireNonNull(parameterId, "parameterId must not be null");
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        paramCode = requireText(paramCode, "paramCode");
        Objects.requireNonNull(paramType, "paramType must not be null");
        defaultValue = normalizeNullableText(defaultValue);
        validationRule = validationRule == null ? ValidationRule.none() : validationRule;
        description = normalizeNullableText(description);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    public boolean pageable() {
        return paramType == ParameterType.PAGEABLE;
    }

    public DataServiceViews.ParameterView toView() {
        return new DataServiceViews.ParameterView(
                parameterId,
                paramCode,
                paramType,
                required,
                defaultValue,
                validationRule,
                enabled,
                description,
                sortOrder
        );
    }

    public enum ParameterType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        JSON,
        PAGEABLE
    }

    public record ValidationRule(
            Integer minLength,
            Integer maxLength,
            BigDecimal minValue,
            BigDecimal maxValue,
            String regex,
            List<String> allowedValues,
            Integer maxPageSize
    ) {

        public ValidationRule {
            if (minLength != null && minLength < 0) {
                throw new IllegalArgumentException("minLength must not be negative");
            }
            if (maxLength != null && maxLength < 0) {
                throw new IllegalArgumentException("maxLength must not be negative");
            }
            if (minLength != null && maxLength != null && minLength > maxLength) {
                throw new IllegalArgumentException("minLength must be less than or equal to maxLength");
            }
            if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException("minValue must be less than or equal to maxValue");
            }
            if (maxPageSize != null && maxPageSize < 1) {
                throw new IllegalArgumentException("maxPageSize must be greater than 0");
            }
            regex = normalizeNullableText(regex);
            allowedValues = immutableAllowedValues(allowedValues);
        }

        public static ValidationRule none() {
            return new ValidationRule(null, null, null, null, null, List.of(), null);
        }

        private static List<String> immutableAllowedValues(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            Set<String> normalized = new LinkedHashSet<>();
            for (String value : values) {
                String candidate = normalizeNullableText(value);
                if (candidate != null) {
                    normalized.add(candidate);
                }
            }
            return List.copyOf(normalized);
        }
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
