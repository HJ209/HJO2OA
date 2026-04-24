package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalTemplateDeprecatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String templateId,
        int versionNo
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.template.deprecated";

    public PortalTemplateDeprecatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        templateId = requireText(templateId, "templateId");
        if (versionNo <= 0) {
            throw new IllegalArgumentException("versionNo must be greater than 0");
        }
    }

    public static PortalTemplateDeprecatedEvent from(
            PortalTemplate template,
            int versionNo,
            Instant occurredAt
    ) {
        return new PortalTemplateDeprecatedEvent(
                UUID.randomUUID(),
                occurredAt,
                template.tenantId(),
                template.templateId(),
                versionNo
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
