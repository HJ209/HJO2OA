package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;
import java.util.Objects;

public record PortalCardSnapshot<T>(
        PortalAggregationSnapshotKey snapshotKey,
        PortalCardType cardType,
        PortalCardState state,
        T data,
        String message,
        Instant refreshedAt
) {

    public PortalCardSnapshot {
        Objects.requireNonNull(snapshotKey, "snapshotKey must not be null");
        Objects.requireNonNull(cardType, "cardType must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
    }

    public static <T> PortalCardSnapshot<T> ready(
            PortalAggregationSnapshotKey snapshotKey,
            PortalCardType cardType,
            T data,
            Instant refreshedAt
    ) {
        return new PortalCardSnapshot<>(snapshotKey, cardType, PortalCardState.READY, data, null, refreshedAt);
    }

    public static <T> PortalCardSnapshot<T> failed(
            PortalAggregationSnapshotKey snapshotKey,
            PortalCardType cardType,
            T data,
            String message,
            Instant refreshedAt
    ) {
        return new PortalCardSnapshot<>(snapshotKey, cardType, PortalCardState.FAILED, data, message, refreshedAt);
    }

    public PortalCardSnapshot<T> markStale(String message, Instant refreshedAt) {
        return new PortalCardSnapshot<>(snapshotKey, cardType, PortalCardState.STALE, data, message, refreshedAt);
    }

    public PortalCardSnapshot<T> markReady(Instant refreshedAt) {
        return new PortalCardSnapshot<>(snapshotKey, cardType, PortalCardState.READY, data, null, refreshedAt);
    }

    public boolean isReady() {
        return state == PortalCardState.READY;
    }

    public boolean isStale() {
        return state == PortalCardState.STALE;
    }

    public boolean isFailed() {
        return state == PortalCardState.FAILED;
    }
}
