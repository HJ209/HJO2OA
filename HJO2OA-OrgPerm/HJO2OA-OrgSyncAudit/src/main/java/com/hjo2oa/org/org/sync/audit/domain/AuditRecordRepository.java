package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditRecordRepository {

    AuditRecord save(AuditRecord auditRecord);

    List<AuditRecord> findByQuery(
            UUID tenantId,
            AuditCategory category,
            String entityType,
            String entityId,
            UUID taskId,
            Instant from,
            Instant to
    );
}
