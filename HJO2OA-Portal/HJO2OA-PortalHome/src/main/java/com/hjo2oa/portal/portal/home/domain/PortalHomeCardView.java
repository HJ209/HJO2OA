package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import java.util.Objects;

public record PortalHomeCardView(
        String cardCode,
        PortalCardType cardType,
        String title,
        String description,
        String actionLink,
        PortalCardState state,
        String message,
        Object data
) {

    public PortalHomeCardView {
        cardCode = requireText(cardCode, "cardCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        title = requireText(title, "title");
        description = requireText(description, "description");
        actionLink = requireText(actionLink, "actionLink");
        Objects.requireNonNull(state, "state must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
