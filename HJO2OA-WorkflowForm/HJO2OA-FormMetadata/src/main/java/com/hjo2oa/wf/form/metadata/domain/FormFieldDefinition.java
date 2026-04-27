package com.hjo2oa.wf.form.metadata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record FormFieldDefinition(
        String fieldCode,
        String fieldName,
        String fieldNameI18nKey,
        FormFieldType fieldType,
        boolean required,
        JsonNode defaultValue,
        String dictionaryCode,
        boolean multiValue,
        boolean visible,
        boolean editable,
        Integer maxLength,
        BigDecimal min,
        BigDecimal max,
        String pattern,
        List<FormFieldDefinition> childFields,
        JsonNode linkageRules
) {

    public FormFieldDefinition {
        fieldCode = requireText(fieldCode, "fieldCode");
        fieldName = requireText(fieldName, "fieldName");
        fieldNameI18nKey = normalizeNullable(fieldNameI18nKey);
        Objects.requireNonNull(fieldType, "fieldType must not be null");
        dictionaryCode = normalizeNullable(dictionaryCode);
        pattern = normalizeNullable(pattern);
        childFields = immutableChildFields(childFields);
        validateDictionary(fieldType, dictionaryCode);
        validateTableChildren(fieldType, childFields);
        if (maxLength != null && maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must not be greater than max");
        }
    }

    public List<FormFieldDefinition> flatten() {
        List<FormFieldDefinition> flattened = new ArrayList<>();
        collect(this, flattened);
        return List.copyOf(flattened);
    }

    private static void collect(FormFieldDefinition field, List<FormFieldDefinition> target) {
        target.add(field);
        for (FormFieldDefinition childField : field.childFields()) {
            collect(childField, target);
        }
    }

    private static List<FormFieldDefinition> immutableChildFields(List<FormFieldDefinition> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        List<FormFieldDefinition> normalized = new ArrayList<>();
        for (FormFieldDefinition field : fields) {
            if (field == null) {
                continue;
            }
            if (!codes.add(field.fieldCode())) {
                throw new IllegalArgumentException("duplicate child fieldCode: " + field.fieldCode());
            }
            normalized.add(field);
        }
        return List.copyOf(normalized);
    }

    private static void validateDictionary(FormFieldType fieldType, String dictionaryCode) {
        if ((fieldType == FormFieldType.SELECT || fieldType == FormFieldType.MULTI_SELECT)
                && dictionaryCode == null) {
            throw new IllegalArgumentException("dictionaryCode is required for select field");
        }
    }

    private static void validateTableChildren(FormFieldType fieldType, List<FormFieldDefinition> childFields) {
        if (fieldType == FormFieldType.TABLE && childFields.isEmpty()) {
            throw new IllegalArgumentException("table field must contain childFields");
        }
        if (fieldType != FormFieldType.TABLE && !childFields.isEmpty()) {
            throw new IllegalArgumentException("only table field can contain childFields");
        }
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
