package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import java.util.Objects;

public record PortalHomeCardTemplate(
        String cardCode,
        PortalCardType cardType,
        String title,
        String description,
        String actionLink,
        String sourcePlacementCode,
        String sourceWidgetCode
) {

    public PortalHomeCardTemplate(
            String cardCode,
            PortalCardType cardType,
            String title,
            String description,
            String actionLink
    ) {
        this(cardCode, cardType, title, description, actionLink, null, null);
    }

    public PortalHomeCardTemplate {
        cardCode = requireText(cardCode, "cardCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        title = requireText(title, "title");
        description = requireText(description, "description");
        actionLink = requireText(actionLink, "actionLink");
        sourcePlacementCode = normalizeOptional(sourcePlacementCode);
        sourceWidgetCode = normalizeOptional(sourceWidgetCode);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
