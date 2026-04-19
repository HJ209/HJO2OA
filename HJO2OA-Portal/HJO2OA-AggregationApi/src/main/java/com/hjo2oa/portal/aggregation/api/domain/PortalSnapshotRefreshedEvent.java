package com.hjo2oa.portal.aggregation.api.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PortalSnapshotRefreshedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String snapshotKey,
        PortalSceneType sceneType,
        PortalCardType cardType,
        Instant refreshedAt
) implements DomainEvent {

    public static final String EVENT_TYPE = "portal.snapshot.refreshed";

    public PortalSnapshotRefreshedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        snapshotKey = requireText(snapshotKey, "snapshotKey");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(cardType, "cardType must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
    }

    public static PortalSnapshotRefreshedEvent from(PortalCardSnapshot<?> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        PortalAggregationSnapshotKey snapshotKey = snapshot.snapshotKey();
        return new PortalSnapshotRefreshedEvent(
                UUID.randomUUID(),
                snapshot.refreshedAt(),
                snapshotKey.tenantId(),
                snapshotKey.asCacheKey(),
                snapshotKey.sceneType(),
                snapshot.cardType(),
                snapshot.refreshedAt()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
