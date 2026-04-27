package com.hjo2oa.org.org.sync.audit.domain;

import java.util.List;
import java.util.UUID;

public interface ConflictRecordRepository {

    ConflictRecord save(ConflictRecord conflictRecord);

    List<ConflictRecord> findByDiffRecordId(UUID tenantId, UUID diffRecordId);
}
