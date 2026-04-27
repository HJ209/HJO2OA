package com.hjo2oa.wf.form.renderer.domain;

import java.util.List;

public record FormValidationResultView(
        boolean valid,
        List<ValidationErrorView> errors
) {

    public FormValidationResultView {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
