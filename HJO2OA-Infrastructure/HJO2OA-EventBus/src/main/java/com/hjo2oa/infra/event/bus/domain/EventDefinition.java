package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EventDefinition(
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
        List<SubscriptionBinding> subscriptions
) {

    public EventDefinition {
        Objects.requireNonNull(id, "id must not be null");
        eventType = requireText(eventType, "eventType");
        modulePrefix = requireText(modulePrefix, "modulePrefix");
        version = requireText(version, "version");
        Objects.requireNonNull(payloadSchema, "payloadSchema must not be null");
        Objects.requireNonNull(publishMode, "publishMode must not be null");
        Objects.requireNonNull(status, "status must not be null");
        ownerModule = requireText(ownerModule, "ownerModule");
        Objects.requireNonNull(tenantScope, "tenantScope must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        subscriptions = subscriptions == null ? List.of() : List.copyOf(subscriptions);
    }

    public static EventDefinition create(
            UUID id,
            String eventType,
            String modulePrefix,
            String version,
            String payloadSchema,
            String description,
            PublishMode publishMode,
            String ownerModule,
            TenantScope tenantScope,
            Instant now
    ) {
        return new EventDefinition(
                id,
                eventType,
                modulePrefix,
                version,
                payloadSchema,
                description,
                publishMode,
                EventDefinitionStatus.DRAFT,
                ownerModule,
                tenantScope,
                now,
                now,
                List.of()
        );
    }

    public EventDefinition activate(Instant now) {
        if (status == EventDefinitionStatus.ACTIVE) {
            return this;
        }
        return new EventDefinition(
                id, eventType, modulePrefix, version, payloadSchema, description,
                publishMode, EventDefinitionStatus.ACTIVE, ownerModule, tenantScope,
                createdAt, now, subscriptions
        );
    }

    public EventDefinition deprecate(Instant now) {
        if (status == EventDefinitionStatus.DEPRECATED) {
            return this;
        }
        return new EventDefinition(
                id, eventType, modulePrefix, version, payloadSchema, description,
                publishMode, EventDefinitionStatus.DEPRECATED, ownerModule, tenantScope,
                createdAt, now, subscriptions
        );
    }

    public EventDefinition addSubscription(SubscriptionBinding subscription, Instant now) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        List<SubscriptionBinding> updated = new ArrayList<>(subscriptions);
        updated.add(subscription);
        return new EventDefinition(
                id, eventType, modulePrefix, version, payloadSchema, description,
                publishMode, status, ownerModule, tenantScope,
                createdAt, now, updated
        );
    }

    public EventDefinitionView toView() {
        return new EventDefinitionView(
                id, eventType, modulePrefix, version, payloadSchema, description,
                publishMode, status, ownerModule, tenantScope,
                createdAt, updatedAt,
                subscriptions.stream().map(SubscriptionBinding::toView).toList()
        );
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
