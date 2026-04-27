package com.hjo2oa.infra.event.bus.domain;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionBinding(
        UUID id,
        UUID eventDefinitionId,
        String subscriberCode,
        MatchMode matchMode,
        String retryPolicy,
        boolean deadLetterEnabled,
        boolean active
) {

    public SubscriptionBinding {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventDefinitionId, "eventDefinitionId must not be null");
        subscriberCode = requireText(subscriberCode, "subscriberCode");
        Objects.requireNonNull(matchMode, "matchMode must not be null");
    }

    public static SubscriptionBinding create(
            UUID id,
            UUID eventDefinitionId,
            String subscriberCode,
            MatchMode matchMode,
            String retryPolicy,
            boolean deadLetterEnabled
    ) {
        return new SubscriptionBinding(id, eventDefinitionId, subscriberCode, matchMode, retryPolicy, deadLetterEnabled, true);
    }

    public SubscriptionBinding deactivate() {
        if (!active) {
            return this;
        }
        return new SubscriptionBinding(id, eventDefinitionId, subscriberCode, matchMode, retryPolicy, deadLetterEnabled, false);
    }

    public SubscriptionBindingView toView() {
        return new SubscriptionBindingView(id, eventDefinitionId, subscriberCode, matchMode, retryPolicy, deadLetterEnabled, active);
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
