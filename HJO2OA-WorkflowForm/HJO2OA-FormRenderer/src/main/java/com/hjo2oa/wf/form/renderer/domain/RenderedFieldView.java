package com.hjo2oa.wf.form.renderer.domain;

import java.math.BigDecimal;
import java.util.List;

public record RenderedFieldView(
        String fieldCode,
        String fieldName,
        String displayName,
        FieldType fieldType,
        Object value,
        String dictionaryCode,
        boolean multiValue,
        boolean visible,
        boolean editable,
        boolean required,
        Integer maxLength,
        BigDecimal min,
        BigDecimal max,
        String pattern,
        List<RenderedFieldView> childFields,
        Object linkageRules
) {

    public RenderedFieldView {
        childFields = childFields == null ? List.of() : List.copyOf(childFields);
    }
}
