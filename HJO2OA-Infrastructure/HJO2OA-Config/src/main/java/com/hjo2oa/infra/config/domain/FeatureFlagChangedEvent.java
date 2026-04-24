package com.hjo2oa.infra.config.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FeatureFlagChangedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        UUID configEntryId,
        String configKey,
        UUID ruleId,
        FeatureRuleType ruleType,
        String changeType,
        int sortOrder
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.feature-flag.changed";

    public FeatureFlagChangedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(configEntryId, "configEntryId must not be null");
        configKey = requireText(configKey, "configKey");
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        changeType = requireText(changeType, "changeType");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    public static FeatureFlagChangedEvent from(
            ConfigEntry entry,
            FeatureRule featureRule,
            String changeType,
            Instant occurredAt
    ) {
        return new FeatureFlagChangedEvent(
                UUID.randomUUID(),
                occurredAt,
                null,
                entry.id(),
                entry.configKey(),
                featureRule.id(),
                featureRule.ruleType(),
                changeType,
                featureRule.sortOrder()
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
