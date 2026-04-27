package com.hjo2oa.org.org.sync.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompensationRecordRepository {

    CompensationRecord save(CompensationRecord compensationRecord);

    Optional<CompensationRecord> findById(UUID compensationRecordId);

    List<CompensationRecord> findByTenantIdAndTaskId(UUID tenantId, UUID taskId);
}
