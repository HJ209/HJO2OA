package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalPublicationOfflinedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String publicationId,
        String templateId,
        PortalPublicationSceneType sceneType
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.publication.offlined";

    public PortalPublicationOfflinedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        publicationId = requireText(publicationId, "publicationId");
        templateId = requireText(templateId, "templateId");
    }

    public static PortalPublicationOfflinedEvent from(PortalPublication publication, Instant occurredAt) {
        return new PortalPublicationOfflinedEvent(
                UUID.randomUUID(),
                occurredAt,
                publication.tenantId(),
                publication.publicationId(),
                publication.templateId(),
                publication.sceneType()
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
