package com.hjo2oa.wf.form.renderer.domain;

public record FieldPermission(
        Boolean visible,
        Boolean editable,
        Boolean required
) {

    public boolean resolveVisible(boolean defaultValue) {
        return visible == null ? defaultValue : visible;
    }

    public boolean resolveEditable(boolean defaultValue) {
        return editable == null ? defaultValue : editable;
    }

    public boolean resolveRequired(boolean defaultValue) {
        return required == null ? defaultValue : required;
    }
}
