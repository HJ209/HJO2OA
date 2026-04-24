package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.util.Map;
import java.util.Objects;

public record PortalWidgetPlacement(
        String placementId,
        String placementCode,
        String widgetCode,
        WidgetCardType cardType,
        int orderNo,
        boolean hiddenByDefault,
        boolean collapsedByDefault,
        Map<String, String> overrideProps
) {

    public PortalWidgetPlacement {
        placementId = requireText(placementId, "placementId");
        placementCode = requireText(placementCode, "placementCode");
        widgetCode = requireText(widgetCode, "widgetCode");
        Objects.requireNonNull(cardType, "cardType must not be null");
        if (orderNo <= 0) {
            throw new IllegalArgumentException("orderNo must be greater than 0");
        }
        overrideProps = Map.copyOf(Objects.requireNonNullElse(overrideProps, Map.of()));
    }

    public PortalWidgetPlacementView toView() {
        return new PortalWidgetPlacementView(
                placementId,
                placementCode,
                widgetCode,
                cardType,
                orderNo,
                hiddenByDefault,
                collapsedByDefault,
                overrideProps
        );
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
