package com.hjo2oa.org.org.sync.audit.interfaces;

import com.hjo2oa.org.org.sync.audit.application.OrgSyncAuditCommands;
import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.CompensationStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffType;
import com.hjo2oa.org.org.sync.audit.domain.SourceStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class OrgSyncAuditDtos {

    private OrgSyncAuditDtos() {
    }

    public record CreateSourceRequest(
            @NotNull UUID tenantId,
            @NotBlank @Size(max = 64) String sourceCode,
            @NotBlank @Size(max = 128) String sourceName,
            @NotBlank @Size(max = 32) String sourceType,
            @Size(max = 512) String endpoint,
            @NotBlank @Size(max = 256) String configRef,
            @Size(max = 1000) String scopeExpression,
            UUID operatorId
    ) {

        public OrgSyncAuditCommands.CreateSourceCommand toCommand() {
            return new OrgSyncAuditCommands.CreateSourceCommand(
                    tenantId,
                    sourceCode,
                    sourceName,
                    sourceType,
                    endpoint,
                    configRef,
                    scopeExpression,
                    operatorId
            );
        }
    }

    public record UpdateSourceRequest(
            @NotBlank @Size(max = 128) String sourceName,
            @NotBlank @Size(max = 32) String sourceType,
            @Size(max = 512) String endpoint,
            @NotBlank @Size(max = 256) String configRef,
            @Size(max = 1000) String scopeExpression,
            UUID operatorId
    ) {

        public OrgSyncAuditCommands.UpdateSourceCommand toCommand(UUID sourceId) {
            return new OrgSyncAuditCommands.UpdateSourceCommand(
                    sourceId,
                    sourceName,
                    sourceType,
                    endpoint,
                    configRef,
                    scopeExpression,
                    operatorId
            );
        }
    }

    public record OperatorRequest(UUID operatorId) {
    }

    public record StartTaskRequest(
            @NotNull UUID tenantId,
            @NotNull UUID sourceId,
            @Size(max = 64) String triggerSource,
            UUID operatorId
    ) {

        public OrgSyncAuditCommands.StartTaskCommand toCommand() {
            return new OrgSyncAuditCommands.StartTaskCommand(tenantId, sourceId, triggerSource, operatorId);
        }
    }

    public record CreateDiffRequest(
            @NotNull UUID tenantId,
            @NotNull UUID taskId,
            @NotBlank @Size(max = 64) String entityType,
            @NotBlank @Size(max = 128) String entityKey,
            @NotNull DiffType diffType,
            String sourceSnapshot,
            String localSnapshot,
            @Size(max = 1000) String suggestion
    ) {

        public OrgSyncAuditCommands.CreateDiffCommand toCommand() {
            return new OrgSyncAuditCommands.CreateDiffCommand(
                    tenantId,
                    taskId,
                    entityType,
                    entityKey,
                    diffType,
                    sourceSnapshot,
                    localSnapshot,
                    suggestion
            );
        }
    }

    public record ResolveDiffRequest(
            @NotBlank @Size(max = 64) String actionType,
            String requestPayload,
            UUID operatorId
    ) {

        public OrgSyncAuditCommands.ResolveDiffCommand toCommand(UUID diffRecordId) {
            return new OrgSyncAuditCommands.ResolveDiffCommand(
                    diffRecordId,
                    actionType,
                    requestPayload,
                    operatorId
            );
        }
    }

    public record SourceResponse(
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

    public record TaskResponse(
            UUID id,
            UUID tenantId,
            UUID sourceId,
            SyncTaskType taskType,
            SyncTaskStatus status,
            UUID retryOfTaskId,
            String triggerSource,
            UUID operatorId,
            Instant startedAt,
            Instant finishedAt,
            int successCount,
            int failureCount,
            int diffCount,
            String failureReason,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DiffResponse(
            UUID id,
            UUID tenantId,
            UUID taskId,
            String entityType,
            String entityKey,
            DiffType diffType,
            DiffStatus status,
            String sourceSnapshot,
            String localSnapshot,
            String suggestion,
            UUID resolvedBy,
            Instant resolvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CompensationResponse(
            UUID id,
            UUID tenantId,
            UUID taskId,
            UUID diffRecordId,
            String actionType,
            CompensationStatus status,
            String requestPayload,
            String resultPayload,
            UUID operatorId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AuditResponse(
            UUID id,
            UUID tenantId,
            AuditCategory category,
            String actionType,
            String entityType,
            String entityId,
            UUID taskId,
            String triggerSource,
            UUID operatorId,
            String beforeSnapshot,
            String afterSnapshot,
            String summary,
            Instant occurredAt,
            Instant createdAt
    ) {
    }
}
