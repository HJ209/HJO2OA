package com.hjo2oa.portal.portal.home.application;

import java.util.List;

public record PortalHomePersonalizationOverlay(
        String basePublicationId,
        List<String> widgetOrderOverride,
        List<String> hiddenPlacementCodes
) {

    public PortalHomePersonalizationOverlay {
        basePublicationId = normalizeOptional(basePublicationId);
        widgetOrderOverride = widgetOrderOverride == null ? List.of() : List.copyOf(widgetOrderOverride);
        hiddenPlacementCodes = hiddenPlacementCodes == null ? List.of() : List.copyOf(hiddenPlacementCodes);
    }

    public static PortalHomePersonalizationOverlay none() {
        return new PortalHomePersonalizationOverlay(null, List.of(), List.of());
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
