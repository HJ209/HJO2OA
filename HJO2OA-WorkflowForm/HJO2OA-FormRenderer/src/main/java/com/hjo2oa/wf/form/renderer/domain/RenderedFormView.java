package com.hjo2oa.wf.form.renderer.domain;

import java.util.List;
import java.util.UUID;

public record RenderedFormView(
        UUID metadataId,
        String code,
        String name,
        String displayName,
        Integer version,
        String nodeId,
        String locale,
        UUID processInstanceId,
        UUID formDataId,
        Object layout,
        List<RenderedFieldView> fields,
        FormValidationResultView validation
) {

    public RenderedFormView {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
