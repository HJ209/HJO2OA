package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import java.util.Objects;

public record PortalHomeCardTemplate(
        String cardCode,
        PortalCardType cardType,
        String title,
        String description,
        String actionLink
) {

    public PortalHomeCardTemplate {
        cardCode = requireText(cardCode, "cardCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        title = requireText(title, "title");
        description = requireText(description, "description");
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
