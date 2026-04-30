package com.hjo2oa.wf.form.metadata.domain;

public record FormMetadataValidationIssue(
        String path,
        String code,
        String message
) {

    public FormMetadataValidationIssue {
        path = FormFieldDefinition.requireText(path, "path");
        code = FormFieldDefinition.requireText(code, "code");
        message = FormFieldDefinition.requireText(message, "message");
    }
}
