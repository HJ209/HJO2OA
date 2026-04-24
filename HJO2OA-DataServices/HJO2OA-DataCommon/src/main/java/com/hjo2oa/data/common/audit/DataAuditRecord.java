package com.hjo2oa.data.common.audit;

import com.hjo2oa.data.common.support.Require;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DataAuditRecord(
        UUID auditId,
        Instant occurredAt,
        String tenantId,
        String moduleCode,
        String objectType,
        String objectId,
        String action,
        String operatorId,
        String summary,
        List<DataAuditFieldChange> fieldChanges
) {

    public DataAuditRecord {
        auditId = Require.nonNull(auditId, "auditId");
        occurredAt = Require.nonNull(occurredAt, "occurredAt");
        tenantId = Require.text(tenantId, "tenantId");
        moduleCode = Require.text(moduleCode, "moduleCode");
        objectType = Require.text(objectType, "objectType");
        objectId = Require.text(objectId, "objectId");
        action = Require.text(action, "action");
        operatorId = Require.text(operatorId, "operatorId");
        summary = Require.text(summary, "summary");
        fieldChanges = List.copyOf(Require.nonNull(fieldChanges, "fieldChanges"));
    }
}
