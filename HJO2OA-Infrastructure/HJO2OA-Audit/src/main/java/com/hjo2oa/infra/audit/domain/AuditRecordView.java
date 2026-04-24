package com.hjo2oa.infra.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditRecordView(
        UUID id,
        String moduleCode,
        String objectType,
        String objectId,
        String actionType,
        UUID operatorAccountId,
        UUID operatorPersonId,
        UUID tenantId,
        String traceId,
        String summary,
        Instant occurredAt,
        ArchiveStatus archiveStatus,
        Instant createdAt,
        List<AuditFieldChangeView> fieldChanges
) {

    public AuditRecordView {
        fieldChanges = fieldChanges == null ? List.of() : List.copyOf(fieldChanges);
    }
}
