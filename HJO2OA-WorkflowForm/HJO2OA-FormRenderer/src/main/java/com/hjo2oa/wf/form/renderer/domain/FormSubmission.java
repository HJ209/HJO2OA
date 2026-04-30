package com.hjo2oa.wf.form.renderer.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record FormSubmission(
        UUID submissionId,
        UUID metadataId,
        String metadataCode,
        int metadataVersion,
        UUID processInstanceId,
        UUID formDataId,
        String nodeId,
        FormSubmissionStatus status,
        Map<String, Object> formData,
        List<String> attachmentIds,
        FormValidationResultView validation,
        String idempotencyKey,
        UUID submittedBy,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt
) {

    public FormSubmission {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(metadataId, "metadataId must not be null");
        metadataCode = requireText(metadataCode, "metadataCode");
        if (metadataVersion < 1) {
            throw new IllegalArgumentException("metadataVersion must be positive");
        }
        Objects.requireNonNull(status, "status must not be null");
        formData = formData == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(formData));
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        Objects.requireNonNull(validation, "validation must not be null");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(submittedBy, "submittedBy must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status == FormSubmissionStatus.SUBMITTED && submittedAt == null) {
            throw new IllegalArgumentException("submittedAt is required for submitted form");
        }
    }

    public static FormSubmission draft(
            UUID submissionId,
            FormMetadataSnapshot snapshot,
            UUID processInstanceId,
            UUID formDataId,
            String nodeId,
            Map<String, Object> formData,
            List<String> attachmentIds,
            FormValidationResultView validation,
            String idempotencyKey,
            UUID submittedBy,
            Instant now
    ) {
        return new FormSubmission(
                submissionId,
                snapshot.metadataId(),
                snapshot.code(),
                snapshot.version(),
                processInstanceId,
                formDataId,
                nodeId,
                FormSubmissionStatus.DRAFT,
                formData,
                attachmentIds,
                validation,
                idempotencyKey,
                submittedBy,
                snapshot.tenantId(),
                now,
                now,
                null
        );
    }

    public FormSubmission updateDraft(
            Map<String, Object> nextFormData,
            List<String> nextAttachmentIds,
            FormValidationResultView nextValidation,
            String nextIdempotencyKey,
            Instant now
    ) {
        if (status != FormSubmissionStatus.DRAFT) {
            throw new IllegalStateException("only draft form submission can be updated");
        }
        return new FormSubmission(
                submissionId,
                metadataId,
                metadataCode,
                metadataVersion,
                processInstanceId,
                formDataId,
                nodeId,
                status,
                nextFormData,
                nextAttachmentIds,
                nextValidation,
                nextIdempotencyKey,
                submittedBy,
                tenantId,
                createdAt,
                now,
                submittedAt
        );
    }

    public FormSubmission submit(FormValidationResultView nextValidation, String nextIdempotencyKey, Instant now) {
        if (status == FormSubmissionStatus.SUBMITTED) {
            return this;
        }
        if (!nextValidation.valid()) {
            throw new IllegalStateException("form submission validation failed");
        }
        return new FormSubmission(
                submissionId,
                metadataId,
                metadataCode,
                metadataVersion,
                processInstanceId,
                formDataId,
                nodeId,
                FormSubmissionStatus.SUBMITTED,
                formData,
                attachmentIds,
                nextValidation,
                nextIdempotencyKey,
                submittedBy,
                tenantId,
                createdAt,
                now,
                now
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
