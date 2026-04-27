package com.hjo2oa.wf.form.renderer.domain;

public record ValidationRule(
        String fieldCode,
        String type,
        String expression,
        String message
) {
}
