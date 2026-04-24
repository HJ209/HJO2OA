package com.hjo2oa.infra.tenant.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantQuotaWarningEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        QuotaType quotaType,
        Long warningThreshold
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.tenant.quota-warning";

    public TenantQuotaWarningEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        Objects.requireNonNull(quotaType, "quotaType must not be null");
    }

    public static TenantQuotaWarningEvent from(TenantQuota quota, Instant occurredAt) {
        return new TenantQuotaWarningEvent(
                UUID.randomUUID(),
                occurredAt,
                quota.tenantProfileId().toString(),
                quota.quotaType(),
                quota.warningThreshold()
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
