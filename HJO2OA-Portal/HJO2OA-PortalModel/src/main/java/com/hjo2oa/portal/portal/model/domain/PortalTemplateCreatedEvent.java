package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalTemplateCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.template.created";

    public PortalTemplateCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        templateId = requireText(templateId, "templateId");
        templateCode = requireText(templateCode, "templateCode");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
    }

    public static PortalTemplateCreatedEvent from(PortalTemplate template, Instant occurredAt) {
        return new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                occurredAt,
                template.tenantId(),
                template.templateId(),
                template.templateCode(),
                template.sceneType()
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
