package com.hjo2oa.portal.portal.model.domain;

import java.util.Objects;

public record PortalWidgetReferenceViolation(
        String widgetCode,
        String placementCode,
        String pageCode,
        String regionCode
) {

    public PortalWidgetReferenceViolation {
        widgetCode = requireText(widgetCode, "widgetCode");
        placementCode = requireText(placementCode, "placementCode");
        pageCode = requireText(pageCode, "pageCode");
        regionCode = requireText(regionCode, "regionCode");
    }

    public String describe() {
        return "widgetCode=" + widgetCode
                + ", placementCode=" + placementCode
                + ", pageCode=" + pageCode
                + ", regionCode=" + regionCode;
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
