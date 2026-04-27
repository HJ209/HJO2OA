package com.hjo2oa.infra.event.bus.domain;

import java.util.UUID;

public record SubscriptionBindingView(
        UUID id,
        UUID eventDefinitionId,
        String subscriberCode,
        MatchMode matchMode,
        String retryPolicy,
        boolean deadLetterEnabled,
        boolean active
) {
}
