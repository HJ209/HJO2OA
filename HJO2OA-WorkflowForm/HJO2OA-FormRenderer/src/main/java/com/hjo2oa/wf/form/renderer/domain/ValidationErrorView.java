package com.hjo2oa.wf.form.renderer.domain;

public record ValidationErrorView(
        String fieldCode,
        String message
) {
}
