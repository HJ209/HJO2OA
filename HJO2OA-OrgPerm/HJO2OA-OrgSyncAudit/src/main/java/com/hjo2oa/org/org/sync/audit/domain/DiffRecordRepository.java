package com.hjo2oa.org.org.sync.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiffRecordRepository {

    DiffRecord save(DiffRecord diffRecord);

    Optional<DiffRecord> findById(UUID diffRecordId);

    List<DiffRecord> findByQuery(UUID tenantId, UUID taskId, DiffStatus status);
}
