package com.hjo2oa.portal.aggregation.api.domain;

import java.util.Objects;

public record PortalAggregationSnapshotKey(
        String tenantId,
        String personId,
        String assignmentId,
        String positionId,
        PortalSceneType sceneType,
        PortalCardType cardType
) {

    public PortalAggregationSnapshotKey {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(cardType, "cardType must not be null");
    }

    public static PortalAggregationSnapshotKey of(
            PortalIdentityCard identityCard,
            PortalSceneType sceneType,
            PortalCardType cardType
    ) {
        Objects.requireNonNull(identityCard, "identityCard must not be null");
        return new PortalAggregationSnapshotKey(
                identityCard.tenantId(),
                identityCard.personId(),
                identityCard.assignmentId(),
                identityCard.positionId(),
                sceneType,
                cardType
        );
    }

    public String asCacheKey() {
        return String.join(
                ":",
                "portal",
                "agg",
                tenantId,
                personId,
                assignmentId,
                positionId,
                sceneType.name(),
                cardType.name()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
