package com.hjo2oa.wf.form.renderer.interfaces;

import com.hjo2oa.wf.form.renderer.application.FormRendererCommands;
import com.hjo2oa.wf.form.renderer.domain.FieldDefinition;
import com.hjo2oa.wf.form.renderer.domain.FieldPermission;
import com.hjo2oa.wf.form.renderer.domain.FieldType;
import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import com.hjo2oa.wf.form.renderer.domain.FormSubmission;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionStatus;
import com.hjo2oa.wf.form.renderer.domain.FormValidationResultView;
import com.hjo2oa.wf.form.renderer.domain.RenderedFieldView;
import com.hjo2oa.wf.form.renderer.domain.ValidationRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FormRendererDtos {

    private FormRendererDtos() {
    }

    public record RenderRequest(
            @NotNull @Valid MetadataSnapshotRequest metadataSnapshot,
            UUID processInstanceId,
            UUID formDataId,
            @Size(max = 64) String nodeId,
            @NotBlank @Size(max = 16) String locale,
            @Size(max = 16) String fallbackLocale,
            Map<String, Object> formData,
            Boolean validateData
    ) {

        public FormRendererCommands.RenderCommand toCommand() {
            return new FormRendererCommands.RenderCommand(
                    metadataSnapshot.toDomain(),
                    processInstanceId,
                    formDataId,
                    nodeId,
                    locale,
                    fallbackLocale,
                    formData == null ? Map.of() : formData,
                    validateData == null || validateData
            );
        }
    }

    public record ValidateRequest(
            @NotNull @Valid MetadataSnapshotRequest metadataSnapshot,
            @Size(max = 64) String nodeId,
            Map<String, Object> formData
    ) {

        public FormRendererCommands.ValidateCommand toCommand() {
            return new FormRendererCommands.ValidateCommand(
                    metadataSnapshot.toDomain(),
                    nodeId,
                    formData == null ? Map.of() : formData
            );
        }
    }

    public record CreateDraftSubmissionRequest(
            @NotNull @Valid MetadataSnapshotRequest metadataSnapshot,
            UUID processInstanceId,
            UUID formDataId,
            @Size(max = 64) String nodeId,
            Map<String, Object> formData,
            @NotNull UUID submittedBy
    ) {

        public FormRendererCommands.CreateDraftCommand toCommand(String idempotencyKey) {
            return new FormRendererCommands.CreateDraftCommand(
                    metadataSnapshot.toDomain(),
                    processInstanceId,
                    formDataId,
                    nodeId,
                    formData == null ? Map.of() : formData,
                    submittedBy,
                    idempotencyKey
            );
        }
    }

    public record UpdateDraftSubmissionRequest(
            @NotNull @Valid MetadataSnapshotRequest metadataSnapshot,
            Map<String, Object> formData
    ) {

        public FormRendererCommands.UpdateDraftCommand toCommand(UUID submissionId, String idempotencyKey) {
            return new FormRendererCommands.UpdateDraftCommand(
                    submissionId,
                    metadataSnapshot.toDomain(),
                    formData == null ? Map.of() : formData,
                    idempotencyKey
            );
        }
    }

    public record SubmitDraftSubmissionRequest(
            @NotNull @Valid MetadataSnapshotRequest metadataSnapshot,
            Map<String, Object> formData
    ) {

        public FormRendererCommands.SubmitDraftCommand toCommand(UUID submissionId, String idempotencyKey) {
            return new FormRendererCommands.SubmitDraftCommand(
                    submissionId,
                    metadataSnapshot.toDomain(),
                    formData == null ? Map.of() : formData,
                    idempotencyKey
            );
        }
    }

    public record MetadataSnapshotRequest(
            @NotNull UUID metadataId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull @Min(1) Integer version,
            @NotEmpty List<@Valid FieldDefinitionRequest> fields,
            Object layout,
            List<@Valid ValidationRuleRequest> validations,
            Map<String, Map<String, @Valid FieldPermissionRequest>> fieldPermissionMap,
            @NotNull UUID tenantId
    ) {

        public FormMetadataSnapshot toDomain() {
            return new FormMetadataSnapshot(
                    metadataId,
                    code,
                    name,
                    version,
                    fields.stream().map(FieldDefinitionRequest::toDomain).toList(),
                    layout,
                    validations == null
                            ? List.of()
                            : validations.stream().map(ValidationRuleRequest::toDomain).toList(),
                    toPermissionMap(fieldPermissionMap),
                    tenantId
            );
        }
    }

    public record FieldDefinitionRequest(
            @NotBlank @Size(max = 128) String fieldCode,
            @NotBlank @Size(max = 128) String fieldName,
            @NotNull FieldType fieldType,
            Boolean required,
            Object defaultValue,
            @Size(max = 64) String dictionaryCode,
            Boolean multiValue,
            Boolean visible,
            Boolean editable,
            Integer maxLength,
            BigDecimal min,
            BigDecimal max,
            @Size(max = 256) String pattern,
            List<@Valid FieldDefinitionRequest> childFields,
            Object linkageRules
    ) {

        public FieldDefinition toDomain() {
            return new FieldDefinition(
                    fieldCode,
                    fieldName,
                    fieldType,
                    required,
                    defaultValue,
                    dictionaryCode,
                    multiValue,
                    visible,
                    editable,
                    maxLength,
                    min,
                    max,
                    pattern,
                    childFields == null
                            ? List.of()
                            : childFields.stream().map(FieldDefinitionRequest::toDomain).toList(),
                    linkageRules
            );
        }
    }

    public record FieldPermissionRequest(
            Boolean visible,
            Boolean editable,
            Boolean required
    ) {

        public FieldPermission toDomain() {
            return new FieldPermission(visible, editable, required);
        }
    }

    public record ValidationRuleRequest(
            @Size(max = 128) String fieldCode,
            @Size(max = 64) String type,
            String expression,
            String message
    ) {

        public ValidationRule toDomain() {
            return new ValidationRule(fieldCode, type, expression, message);
        }
    }

    public record RenderResponse(
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
    }

    public record FormSubmissionResponse(
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
            UUID submittedBy,
            UUID tenantId,
            String createdAt,
            String updatedAt,
            String submittedAt
    ) {

        public static FormSubmissionResponse from(FormSubmission submission) {
            return new FormSubmissionResponse(
                    submission.submissionId(),
                    submission.metadataId(),
                    submission.metadataCode(),
                    submission.metadataVersion(),
                    submission.processInstanceId(),
                    submission.formDataId(),
                    submission.nodeId(),
                    submission.status(),
                    submission.formData(),
                    submission.attachmentIds(),
                    submission.validation(),
                    submission.submittedBy(),
                    submission.tenantId(),
                    submission.createdAt().toString(),
                    submission.updatedAt().toString(),
                    submission.submittedAt() == null ? null : submission.submittedAt().toString()
            );
        }
    }

    private static Map<String, Map<String, FieldPermission>> toPermissionMap(
            Map<String, Map<String, FieldPermissionRequest>> source
    ) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return source.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .entrySet()
                                .stream()
                                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                        Map.Entry::getKey,
                                        permission -> permission.getValue().toDomain()
                                ))
                ));
    }
}
