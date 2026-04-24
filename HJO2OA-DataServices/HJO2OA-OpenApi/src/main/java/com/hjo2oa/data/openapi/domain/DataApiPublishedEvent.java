package com.hjo2oa.data.openapi.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DataApiPublishedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String apiId,
        String code,
        String path,
        OpenApiHttpMethod httpMethod,
        String version,
        OpenApiAuthType authType,
        Instant publishedAt
) implements DomainEvent {

    public static final String EVENT_TYPE = "data.api.published";

    public DataApiPublishedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        apiId = requireText(apiId, "apiId");
        code = requireText(code, "code");
        path = requireText(path, "path");
        Objects.requireNonNull(httpMethod, "httpMethod must not be null");
        version = requireText(version, "version");
        Objects.requireNonNull(authType, "authType must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    }

    public static DataApiPublishedEvent from(OpenApiEndpoint endpoint, Instant occurredAt) {
        return new DataApiPublishedEvent(
                UUID.randomUUID(),
                occurredAt,
                endpoint.tenantId(),
                endpoint.apiId(),
                endpoint.code(),
                endpoint.path(),
                endpoint.httpMethod(),
                endpoint.version(),
                endpoint.authType(),
                endpoint.publishedAt() == null ? occurredAt : endpoint.publishedAt()
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
