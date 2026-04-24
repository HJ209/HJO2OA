package com.hjo2oa.portal.portal.home.domain;

import java.util.Objects;

public record PortalHomeNavigationItem(
        String code,
        String title,
        Long badgeCount,
        String actionLink
) {

    public PortalHomeNavigationItem {
        code = requireText(code, "code");
        title = requireText(title, "title");
        actionLink = requireText(actionLink, "actionLink");
    }

    public PortalHomeNavigationItem(String code, String title, String actionLink) {
        this(code, title, null, actionLink);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
