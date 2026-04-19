package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;
import java.util.List;

public record PortalOfficeCenterView(
        PortalSceneType sceneType,
        List<PortalOfficeCenterNavItem> navigation,
        PortalCardSnapshot<PortalIdentityCard> identity,
        PortalCardSnapshot<PortalTodoCard> todo,
        PortalCardSnapshot<PortalMessageCard> message,
        Instant generatedAt
) {

    public PortalOfficeCenterView {
        navigation = List.copyOf(navigation);
    }
}
