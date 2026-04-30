package com.hjo2oa.wf.form.renderer.application;

import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import java.util.Map;
import java.util.UUID;

public final class FormRendererCommands {

    private FormRendererCommands() {
    }

    public record RenderCommand(
            FormMetadataSnapshot metadataSnapshot,
            UUID processInstanceId,
            UUID formDataId,
            String nodeId,
            String locale,
            String fallbackLocale,
            Map<String, Object> formData,
            boolean validateData
    ) {
    }

    public record ValidateCommand(
            FormMetadataSnapshot metadataSnapshot,
            String nodeId,
            Map<String, Object> formData
    ) {
    }

    public record CreateDraftCommand(
            FormMetadataSnapshot metadataSnapshot,
            UUID processInstanceId,
            UUID formDataId,
            String nodeId,
            Map<String, Object> formData,
            UUID submittedBy,
            String idempotencyKey
    ) {
    }

    public record UpdateDraftCommand(
            UUID submissionId,
            FormMetadataSnapshot metadataSnapshot,
            Map<String, Object> formData,
            String idempotencyKey
    ) {
    }

    public record SubmitDraftCommand(
            UUID submissionId,
            FormMetadataSnapshot metadataSnapshot,
            Map<String, Object> formData,
            String idempotencyKey
    ) {
    }
}
