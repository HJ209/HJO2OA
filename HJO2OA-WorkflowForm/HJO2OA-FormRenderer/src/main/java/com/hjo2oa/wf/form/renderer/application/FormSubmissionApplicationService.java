package com.hjo2oa.wf.form.renderer.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.form.renderer.domain.FieldDefinition;
import com.hjo2oa.wf.form.renderer.domain.FieldType;
import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import com.hjo2oa.wf.form.renderer.domain.FormSubmission;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionRepository;
import com.hjo2oa.wf.form.renderer.domain.FormValidationResultView;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FormSubmissionApplicationService {

    private final FormSubmissionRepository repository;
    private final FormRendererApplicationService rendererApplicationService;
    private final Clock clock;

    @Autowired
    public FormSubmissionApplicationService(
            FormSubmissionRepository repository,
            FormRendererApplicationService rendererApplicationService
    ) {
        this(repository, rendererApplicationService, Clock.systemUTC());
    }

    public FormSubmissionApplicationService(
            FormSubmissionRepository repository,
            FormRendererApplicationService rendererApplicationService,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.rendererApplicationService = Objects.requireNonNull(
                rendererApplicationService,
                "rendererApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public FormSubmission createDraft(FormRendererCommands.CreateDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FormMetadataSnapshot snapshot = requireSnapshot(command.metadataSnapshot());
        String idempotencyKey = requireText(command.idempotencyKey(), "idempotencyKey");
        return repository.findByTenantIdAndIdempotencyKey(snapshot.tenantId(), idempotencyKey)
                .orElseGet(() -> {
                    Map<String, Object> formData = normalizedFormData(command.formData());
                    FormValidationResultView validation = validate(snapshot, command.nodeId(), formData);
                    FormSubmission draft = FormSubmission.draft(
                            UUID.randomUUID(),
                            snapshot,
                            command.processInstanceId(),
                            command.formDataId(),
                            normalize(command.nodeId()),
                            formData,
                            extractAttachmentIds(snapshot.fields(), formData),
                            validation,
                            idempotencyKey,
                            Objects.requireNonNull(command.submittedBy(), "submittedBy must not be null"),
                            now()
                    );
                    return repository.save(draft);
                });
    }

    public FormSubmission updateDraft(FormRendererCommands.UpdateDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FormMetadataSnapshot snapshot = requireSnapshot(command.metadataSnapshot());
        String idempotencyKey = requireText(command.idempotencyKey(), "idempotencyKey");
        return repository.findByTenantIdAndIdempotencyKey(snapshot.tenantId(), idempotencyKey)
                .map(existing -> sameSubmissionOrConflict(existing, command.submissionId()))
                .orElseGet(() -> {
                    FormSubmission current = loadRequired(command.submissionId());
                    ensureSameMetadata(current, snapshot);
                    Map<String, Object> formData = normalizedFormData(command.formData());
                    FormValidationResultView validation = validate(snapshot, current.nodeId(), formData);
                    try {
                        return repository.save(current.updateDraft(
                                formData,
                                extractAttachmentIds(snapshot.fields(), formData),
                                validation,
                                idempotencyKey,
                                now()
                        ));
                    } catch (IllegalStateException ex) {
                        throw new BizException(
                                SharedErrorDescriptors.CONFLICT,
                                "Only draft form submission can be updated",
                                ex
                        );
                    }
                });
    }

    public FormSubmission submitDraft(FormRendererCommands.SubmitDraftCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FormMetadataSnapshot snapshot = requireSnapshot(command.metadataSnapshot());
        String idempotencyKey = requireText(command.idempotencyKey(), "idempotencyKey");
        return repository.findByTenantIdAndIdempotencyKey(snapshot.tenantId(), idempotencyKey)
                .map(existing -> sameSubmissionOrConflict(existing, command.submissionId()))
                .orElseGet(() -> {
                    FormSubmission current = loadRequired(command.submissionId());
                    ensureSameMetadata(current, snapshot);
                    Map<String, Object> nextFormData = command.formData() == null || command.formData().isEmpty()
                            ? current.formData()
                            : normalizedFormData(command.formData());
                    FormValidationResultView validation = validate(snapshot, current.nodeId(), nextFormData);
                    if (!validation.valid()) {
                        throw new BizException(
                                SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                                "Form submission validation failed"
                        );
                    }
                    FormSubmission updated = current.updateDraft(
                            nextFormData,
                            extractAttachmentIds(snapshot.fields(), nextFormData),
                            validation,
                            idempotencyKey,
                            now()
                    );
                    try {
                        return repository.save(updated.submit(validation, idempotencyKey, now()));
                    } catch (IllegalStateException ex) {
                        throw new BizException(
                                SharedErrorDescriptors.CONFLICT,
                                "Only valid draft form submission can be submitted",
                                ex
                        );
                    }
                });
    }

    public FormSubmission get(UUID submissionId) {
        return loadRequired(submissionId);
    }

    private FormSubmission loadRequired(UUID submissionId) {
        return repository.findById(Objects.requireNonNull(submissionId, "submissionId must not be null"))
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Form submission not found"
                ));
    }

    private FormValidationResultView validate(FormMetadataSnapshot snapshot, String nodeId, Map<String, Object> formData) {
        return rendererApplicationService.validateForm(new FormRendererCommands.ValidateCommand(snapshot, nodeId, formData));
    }

    private FormMetadataSnapshot requireSnapshot(FormMetadataSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "metadataSnapshot must not be null");
        Objects.requireNonNull(snapshot.metadataId(), "metadataId must not be null");
        Objects.requireNonNull(snapshot.tenantId(), "tenantId must not be null");
        requireText(snapshot.code(), "metadata code");
        requireText(snapshot.name(), "metadata name");
        if (snapshot.version() == null || snapshot.version() < 1) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "metadata version must be greater than or equal to 1"
            );
        }
        return snapshot;
    }

    private FormSubmission sameSubmissionOrConflict(FormSubmission existing, UUID expectedSubmissionId) {
        if (!existing.submissionId().equals(expectedSubmissionId)) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Idempotency key is already used");
        }
        return existing;
    }

    private void ensureSameMetadata(FormSubmission submission, FormMetadataSnapshot snapshot) {
        if (!submission.metadataId().equals(snapshot.metadataId())
                || submission.metadataVersion() != snapshot.version()
                || !submission.tenantId().equals(snapshot.tenantId())) {
            throw new BizException(
                    SharedErrorDescriptors.CONFLICT,
                    "Form submission metadata snapshot does not match"
            );
        }
    }

    private Map<String, Object> normalizedFormData(Map<String, Object> formData) {
        return formData == null ? Map.of() : formData;
    }

    private List<String> extractAttachmentIds(List<FieldDefinition> fields, Map<String, Object> formData) {
        LinkedHashSet<String> attachmentIds = new LinkedHashSet<>();
        for (FieldDefinition field : fields) {
            collectAttachmentIds(field, formData.get(field.fieldCode()), attachmentIds);
        }
        return new ArrayList<>(attachmentIds);
    }

    private void collectAttachmentIds(FieldDefinition field, Object value, LinkedHashSet<String> attachmentIds) {
        if (field.fieldType() == FieldType.ATTACHMENT || field.fieldType() == FieldType.IMAGE) {
            appendAttachmentValue(value, attachmentIds);
        }
        if (field.fieldType() == FieldType.TABLE && value instanceof List<?> rows) {
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> rowData)) {
                    continue;
                }
                for (FieldDefinition child : field.childFields()) {
                    collectAttachmentIds(child, rowData.get(child.fieldCode()), attachmentIds);
                }
            }
        }
    }

    private void appendAttachmentValue(Object value, LinkedHashSet<String> attachmentIds) {
        if (value instanceof String text && !text.isBlank()) {
            attachmentIds.add(text.trim());
            return;
        }
        if (value instanceof Collection<?> values) {
            for (Object item : values) {
                appendAttachmentValue(item, attachmentIds);
            }
        }
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, fieldName + " must not be blank");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Instant now() {
        return clock.instant();
    }
}
