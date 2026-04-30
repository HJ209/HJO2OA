package com.hjo2oa.portal.aggregation.api.domain;

import java.util.Objects;

public record PortalDashboardView(
        PortalSceneType sceneType,
        PortalCardSnapshot<PortalIdentityCard> identity,
        PortalCardSnapshot<PortalTodoCard> todo,
        PortalCardSnapshot<PortalMessageCard> message,
        PortalCardSnapshot<PortalContentCard> content
) {

    public PortalDashboardView {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
    }
}
