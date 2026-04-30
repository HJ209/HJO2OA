package com.hjo2oa.wf.form.renderer.domain;

import java.util.Optional;
import java.util.UUID;

public interface FormSubmissionRepository {

    Optional<FormSubmission> findById(UUID submissionId);

    Optional<FormSubmission> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    FormSubmission save(FormSubmission submission);
}
