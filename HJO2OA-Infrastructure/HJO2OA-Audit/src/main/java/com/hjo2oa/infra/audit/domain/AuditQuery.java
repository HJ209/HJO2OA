package com.hjo2oa.infra.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditQuery(
        UUID tenantId,
        String moduleCode,
        String objectType,
        String objectId,
        String actionType,
        UUID operatorAccountId,
        UUID operatorPersonId,
        String traceId,
        Instant from,
        Instant to
) {

    public AuditQuery {
        moduleCode = normalizeNullable(moduleCode);
        objectType = normalizeNullable(objectType);
        objectId = normalizeNullable(objectId);
        actionType = normalizeNullable(actionType);
        traceId = normalizeNullable(traceId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
