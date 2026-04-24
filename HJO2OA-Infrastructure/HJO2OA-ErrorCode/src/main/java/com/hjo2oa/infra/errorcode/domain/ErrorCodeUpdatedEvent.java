package com.hjo2oa.infra.errorcode.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ErrorCodeUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        UUID errorCodeId,
        String code,
        String moduleCode,
        String changeType,
        ErrorSeverity severity,
        int httpStatus,
        boolean deprecated
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.error-code.updated";
    private static final String PLATFORM_TENANT_ID = "platform";

    public ErrorCodeUpdatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(errorCodeId, "errorCodeId must not be null");
        code = requireText(code, "code");
        moduleCode = requireText(moduleCode, "moduleCode");
        changeType = requireText(changeType, "changeType");
        Objects.requireNonNull(severity, "severity must not be null");
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("httpStatus must be between 100 and 599");
        }
    }

    public static ErrorCodeUpdatedEvent from(
            ErrorCodeDefinition definition,
            String changeType,
            Instant occurredAt
    ) {
        return new ErrorCodeUpdatedEvent(
                UUID.randomUUID(),
                occurredAt,
                PLATFORM_TENANT_ID,
                definition.id(),
                definition.code(),
                definition.moduleCode(),
                changeType,
                definition.severity(),
                definition.httpStatus(),
                definition.deprecated()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
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
