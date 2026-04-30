package com.hjo2oa.wf.form.renderer.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.form.renderer.domain.FormSubmission;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionRepository;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionStatus;
import com.hjo2oa.wf.form.renderer.domain.FormValidationResultView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisFormSubmissionRepository implements FormSubmissionRepository {

    private static final TypeReference<Map<String, Object>> FORM_DATA_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> ATTACHMENT_IDS_TYPE = new TypeReference<>() {
    };

    private final FormSubmissionMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisFormSubmissionRepository(FormSubmissionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<FormSubmission> findById(UUID submissionId) {
        return Optional.ofNullable(mapper.selectById(submissionId)).map(this::toDomain);
    }

    @Override
    public Optional<FormSubmission> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<FormSubmissionEntity>()
                .eq("tenant_id", tenantId)
                .eq("idempotency_key", idempotencyKey)))
                .map(this::toDomain);
    }

    @Override
    public FormSubmission save(FormSubmission submission) {
        FormSubmissionEntity existing = mapper.selectById(submission.submissionId());
        FormSubmissionEntity entity = toEntity(submission, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(submission.submissionId()).orElseThrow();
    }

    private FormSubmission toDomain(FormSubmissionEntity entity) {
        return new FormSubmission(
                entity.getSubmissionId(),
                entity.getMetadataId(),
                entity.getMetadataCode(),
                entity.getMetadataVersion(),
                entity.getProcessInstanceId(),
                entity.getFormDataId(),
                entity.getNodeId(),
                FormSubmissionStatus.valueOf(entity.getStatus()),
                readValue(entity.getFormData(), FORM_DATA_TYPE),
                readValue(entity.getAttachmentIds(), ATTACHMENT_IDS_TYPE),
                readValue(entity.getValidationResult(), FormValidationResultView.class),
                entity.getIdempotencyKey(),
                entity.getSubmittedBy(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSubmittedAt()
        );
    }

    private FormSubmissionEntity toEntity(FormSubmission submission, FormSubmissionEntity existing) {
        FormSubmissionEntity entity = existing == null ? new FormSubmissionEntity() : existing;
        entity.setSubmissionId(submission.submissionId());
        entity.setMetadataId(submission.metadataId());
        entity.setMetadataCode(submission.metadataCode());
        entity.setMetadataVersion(submission.metadataVersion());
        entity.setProcessInstanceId(submission.processInstanceId());
        entity.setFormDataId(submission.formDataId());
        entity.setNodeId(submission.nodeId());
        entity.setStatus(submission.status().name());
        entity.setFormData(writeValue(submission.formData()));
        entity.setAttachmentIds(writeValue(submission.attachmentIds()));
        entity.setValidationResult(writeValue(submission.validation()));
        entity.setIdempotencyKey(submission.idempotencyKey());
        entity.setSubmittedBy(submission.submittedBy());
        entity.setTenantId(submission.tenantId());
        entity.setCreatedAt(submission.createdAt());
        entity.setUpdatedAt(submission.updatedAt());
        entity.setSubmittedAt(submission.submittedAt());
        return entity;
    }

    private <T> T readValue(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw jsonException(ex);
        }
    }

    private <T> T readValue(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw jsonException(ex);
        }
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw jsonException(ex);
        }
    }

    private BizException jsonException(Exception ex) {
        return new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "Form submission payload serialization failed", ex);
    }
}
