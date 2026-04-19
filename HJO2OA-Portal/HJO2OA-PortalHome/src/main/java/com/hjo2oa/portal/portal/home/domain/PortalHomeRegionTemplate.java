package com.hjo2oa.portal.portal.home.domain;

import java.util.List;
import java.util.Objects;

public record PortalHomeRegionTemplate(
        String regionCode,
        String title,
        String description,
        List<PortalHomeCardTemplate> cards
) {

    public PortalHomeRegionTemplate {
        regionCode = requireText(regionCode, "regionCode");
        title = requireText(title, "title");
        description = requireText(description, "description");
        cards = List.copyOf(Objects.requireNonNullElse(cards, List.of()));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
