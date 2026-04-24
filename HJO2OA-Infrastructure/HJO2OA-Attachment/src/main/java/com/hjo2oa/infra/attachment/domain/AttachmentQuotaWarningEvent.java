package com.hjo2oa.infra.attachment.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AttachmentQuotaWarningEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        UUID attachmentId,
        long threshold
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.attachment.quota-warning";

    public AttachmentQuotaWarningEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(attachmentId, "attachmentId must not be null");
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be greater than 0");
        }
    }

    public static AttachmentQuotaWarningEvent of(
            UUID tenantId,
            UUID attachmentId,
            long threshold,
            Instant occurredAt
    ) {
        return new AttachmentQuotaWarningEvent(
                UUID.randomUUID(),
                occurredAt,
                tenantId.toString(),
                attachmentId,
                threshold
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
