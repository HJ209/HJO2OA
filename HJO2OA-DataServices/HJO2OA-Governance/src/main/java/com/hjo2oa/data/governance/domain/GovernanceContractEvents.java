package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class GovernanceContractEvents {

    private GovernanceContractEvents() {
    }

    public record DataGovernanceAlertedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String governanceId,
            String alertId,
            String targetCode,
            GovernanceScopeType targetType,
            AlertLevel alertLevel,
            String alertType
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.governance.alerted";

        public DataGovernanceAlertedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            governanceId = requireText(governanceId, "governanceId");
            alertId = requireText(alertId, "alertId");
            targetCode = requireText(targetCode, "targetCode");
            Objects.requireNonNull(targetType, "targetType must not be null");
            Objects.requireNonNull(alertLevel, "alertLevel must not be null");
            alertType = requireText(alertType, "alertType");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataApiPublishedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String apiId,
            String code,
            String version,
            String path,
            String httpMethod,
            RuntimeTargetStatus status
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.api.published";

        public DataApiPublishedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            apiId = requireText(apiId, "apiId");
            code = requireText(code, "code");
            version = requireText(version, "version");
            path = requireText(path, "path");
            httpMethod = requireText(httpMethod, "httpMethod");
            status = status == null ? RuntimeTargetStatus.ACTIVE : status;
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataApiDeprecatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String apiId,
            String code,
            String version,
            Instant sunsetAt
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.api.deprecated";

        public DataApiDeprecatedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            apiId = requireText(apiId, "apiId");
            code = requireText(code, "code");
            version = requireText(version, "version");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataConnectorUpdatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String connectorId,
            String code,
            String connectorType,
            RuntimeTargetStatus status
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.connector.updated";

        public DataConnectorUpdatedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            connectorId = requireText(connectorId, "connectorId");
            code = requireText(code, "code");
            connectorType = requireText(connectorType, "connectorType");
            status = status == null ? RuntimeTargetStatus.UNKNOWN : status;
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataSyncCompletedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String syncTaskId,
            String code,
            String executionId,
            long insertedCount,
            long updatedCount,
            long failedCount
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.sync.completed";

        public DataSyncCompletedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            syncTaskId = requireText(syncTaskId, "syncTaskId");
            code = requireText(code, "code");
            executionId = requireText(executionId, "executionId");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataSyncFailedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String syncTaskId,
            String code,
            String executionId,
            String errorCode,
            String errorMessage,
            boolean retryable
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.sync.failed";

        public DataSyncFailedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            syncTaskId = requireText(syncTaskId, "syncTaskId");
            code = requireText(code, "code");
            executionId = requireText(executionId, "executionId");
            errorCode = requireText(errorCode, "errorCode");
            errorMessage = requireText(errorMessage, "errorMessage");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataReportRefreshedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String reportId,
            String code,
            Instant snapshotAt,
            String freshnessStatus
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.report.refreshed";

        public DataReportRefreshedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            reportId = requireText(reportId, "reportId");
            code = requireText(code, "code");
            Objects.requireNonNull(snapshotAt, "snapshotAt must not be null");
            freshnessStatus = requireText(freshnessStatus, "freshnessStatus");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    public record DataServiceActivatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String serviceId,
            String code,
            String serviceType,
            String permissionMode
    ) implements DomainEvent {

        public static final String EVENT_TYPE = "data.service.activated";

        public DataServiceActivatedEvent {
            Objects.requireNonNull(eventId, "eventId must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
            tenantId = requireText(tenantId, "tenantId");
            serviceId = requireText(serviceId, "serviceId");
            code = requireText(code, "code");
            serviceType = requireText(serviceType, "serviceType");
            permissionMode = requireText(permissionMode, "permissionMode");
        }

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
