package com.hjo2oa.wf.form.renderer.domain;

import java.math.BigDecimal;
import java.util.List;

public record FieldDefinition(
        String fieldCode,
        String fieldName,
        FieldType fieldType,
        Boolean required,
        Object defaultValue,
        String dictionaryCode,
        Boolean multiValue,
        Boolean visible,
        Boolean editable,
        Integer maxLength,
        BigDecimal min,
        BigDecimal max,
        String pattern,
        List<FieldDefinition> childFields,
        Object linkageRules
) {

    public FieldDefinition {
        childFields = childFields == null ? List.of() : List.copyOf(childFields);
    }

    public boolean isRequiredByDefault() {
        return Boolean.TRUE.equals(required);
    }

    public boolean isVisibleByDefault() {
        return visible == null || Boolean.TRUE.equals(visible);
    }

    public boolean isEditableByDefault() {
        return editable == null || Boolean.TRUE.equals(editable);
    }

    public boolean isMultiValue() {
        return Boolean.TRUE.equals(multiValue);
    }
}
