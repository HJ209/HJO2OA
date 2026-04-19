package com.hjo2oa.portal.portal.home.domain;

import java.util.Objects;

public record PortalHomeFooter(
        String text
) {

    public PortalHomeFooter {
        Objects.requireNonNull(text, "text must not be null");
        if (text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
