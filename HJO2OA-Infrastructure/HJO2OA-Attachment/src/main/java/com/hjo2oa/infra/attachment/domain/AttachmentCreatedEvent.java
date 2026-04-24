package com.hjo2oa.infra.attachment.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AttachmentCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        UUID attachmentId,
        String fileName,
        StorageProvider storageProvider
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.attachment.created";

    public AttachmentCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(attachmentId, "attachmentId must not be null");
        fileName = requireText(fileName, "fileName");
        Objects.requireNonNull(storageProvider, "storageProvider must not be null");
    }

    public static AttachmentCreatedEvent from(AttachmentAsset attachmentAsset, Instant occurredAt) {
        return new AttachmentCreatedEvent(
                UUID.randomUUID(),
                occurredAt,
                attachmentAsset.tenantId().toString(),
                attachmentAsset.id(),
                attachmentAsset.originalFilename(),
                attachmentAsset.storageProvider()
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
