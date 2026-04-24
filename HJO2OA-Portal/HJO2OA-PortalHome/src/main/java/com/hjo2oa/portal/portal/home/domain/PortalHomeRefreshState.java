package com.hjo2oa.portal.portal.home.domain;

import java.time.Instant;
import java.util.Objects;

public record PortalHomeRefreshState(
        PortalHomeSceneType sceneType,
        PortalHomeRefreshStatus status,
        String triggerEvent,
        String cardType,
        String message,
        Instant updatedAt
) {

    public PortalHomeRefreshState {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        triggerEvent = normalize(triggerEvent);
        cardType = normalize(cardType);
        message = normalize(message);
    }

    public static PortalHomeRefreshState idle(PortalHomeSceneType sceneType, Instant updatedAt) {
        return new PortalHomeRefreshState(sceneType, PortalHomeRefreshStatus.IDLE, null, null, null, updatedAt);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
