package com.hjo2oa.wf.form.renderer.infrastructure;

import com.hjo2oa.wf.form.renderer.domain.FormSubmission;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFormSubmissionRepository implements FormSubmissionRepository {

    private final Map<UUID, FormSubmission> submissions = new ConcurrentHashMap<>();

    @Override
    public Optional<FormSubmission> findById(UUID submissionId) {
        return Optional.ofNullable(submissions.get(submissionId));
    }

    @Override
    public Optional<FormSubmission> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return submissions.values().stream()
                .filter(submission -> submission.tenantId().equals(tenantId))
                .filter(submission -> submission.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public FormSubmission save(FormSubmission submission) {
        submissions.put(submission.submissionId(), submission);
        return submission;
    }
}
