package com.hjo2oa.portal.portal.home.domain;

import java.util.Objects;

public record PortalHomeBranding(
        String title,
        String subtitle,
        String logoText
) {

    public PortalHomeBranding {
        title = requireText(title, "title");
        subtitle = requireText(subtitle, "subtitle");
        logoText = requireText(logoText, "logoText");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
