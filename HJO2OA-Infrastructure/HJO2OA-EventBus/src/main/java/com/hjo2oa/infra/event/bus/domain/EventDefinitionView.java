package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDefinitionView(
        UUID id,
        String eventType,
        String modulePrefix,
        String version,
        String payloadSchema,
        String description,
        PublishMode publishMode,
        EventDefinitionStatus status,
        String ownerModule,
        TenantScope tenantScope,
        Instant createdAt,
        Instant updatedAt,
        List<SubscriptionBindingView> subscriptions
) {
}
