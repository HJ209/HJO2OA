package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record SyncSourceConfigView(
        UUID id,
        UUID tenantId,
        String sourceCode,
        String sourceName,
        String sourceType,
        String endpoint,
        String configRef,
        String scopeExpression,
        SourceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
