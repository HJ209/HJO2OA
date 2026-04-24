package com.hjo2oa.infra.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditRecordRepository {

    AuditRecord save(AuditRecord record);

    Optional<AuditRecord> findById(UUID id);

    List<AuditRecord> findByQuery(AuditQuery query);

    List<AuditRecord> findByTenantAndTimeRange(UUID tenantId, Instant from, Instant to);
}
