package com.hjo2oa.data.openapi.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DataApiDeprecatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String apiId,
        String code,
        String version,
        Instant deprecatedAt,
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
        Objects.requireNonNull(deprecatedAt, "deprecatedAt must not be null");
    }

    public static DataApiDeprecatedEvent from(OpenApiEndpoint endpoint, Instant occurredAt) {
        return new DataApiDeprecatedEvent(
                UUID.randomUUID(),
                occurredAt,
                endpoint.tenantId(),
                endpoint.apiId(),
                endpoint.code(),
                endpoint.version(),
                endpoint.deprecatedAt() == null ? occurredAt : endpoint.deprecatedAt(),
                endpoint.sunsetAt()
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
