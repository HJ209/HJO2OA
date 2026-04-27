package com.hjo2oa.org.org.sync.audit.application;

import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.DiffStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffType;
import java.time.Instant;
import java.util.UUID;

public final class OrgSyncAuditCommands {

    private OrgSyncAuditCommands() {
    }

    public record CreateSourceCommand(
            UUID tenantId,
            String sourceCode,
            String sourceName,
            String sourceType,
            String endpoint,
            String configRef,
            String scopeExpression,
            UUID operatorId
    ) {
    }

    public record UpdateSourceCommand(
            UUID sourceId,
            String sourceName,
            String sourceType,
            String endpoint,
            String configRef,
            String scopeExpression,
            UUID operatorId
    ) {
    }

    public record StartTaskCommand(
            UUID tenantId,
            UUID sourceId,
            String triggerSource,
            UUID operatorId
    ) {
    }

    public record CreateDiffCommand(
            UUID tenantId,
            UUID taskId,
            String entityType,
            String entityKey,
            DiffType diffType,
            String sourceSnapshot,
            String localSnapshot,
            String suggestion
    ) {
    }

    public record ResolveDiffCommand(
            UUID diffRecordId,
            String actionType,
            String requestPayload,
            UUID operatorId
    ) {
    }

    public record DiffQuery(UUID tenantId, UUID taskId, DiffStatus status) {
    }

    public record AuditQuery(
            UUID tenantId,
            AuditCategory category,
            String entityType,
            String entityId,
            UUID taskId,
            Instant from,
            Instant to
    ) {
    }
}
