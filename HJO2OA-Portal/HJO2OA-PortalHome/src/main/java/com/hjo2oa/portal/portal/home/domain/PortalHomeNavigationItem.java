package com.hjo2oa.portal.portal.home.domain;

import java.util.Objects;

public record PortalHomeNavigationItem(
        String code,
        String title,
        String actionLink
) {

    public PortalHomeNavigationItem {
        code = requireText(code, "code");
        title = requireText(title, "title");
        actionLink = requireText(actionLink, "actionLink");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
