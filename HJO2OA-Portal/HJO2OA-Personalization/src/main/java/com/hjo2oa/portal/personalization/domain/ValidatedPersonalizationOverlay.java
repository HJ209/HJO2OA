package com.hjo2oa.portal.personalization.domain;

import java.util.List;
import java.util.Objects;

public record ValidatedPersonalizationOverlay(
        String basePublicationId,
        List<String> widgetOrderOverride,
        List<String> hiddenPlacementCodes
) {

    public ValidatedPersonalizationOverlay {
        basePublicationId = requireText(basePublicationId, "basePublicationId");
        widgetOrderOverride = widgetOrderOverride == null ? List.of() : List.copyOf(widgetOrderOverride);
        hiddenPlacementCodes = hiddenPlacementCodes == null ? List.of() : List.copyOf(hiddenPlacementCodes);
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
