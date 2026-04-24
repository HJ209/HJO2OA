package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.util.Map;

public record PortalWidgetPlacementView(
        String placementId,
        String placementCode,
        String widgetCode,
        WidgetCardType cardType,
        int orderNo,
        boolean hiddenByDefault,
        boolean collapsedByDefault,
        Map<String, String> overrideProps
) {
}
