package com.hjo2oa.portal.portal.home.domain;

import java.util.Optional;

public interface PortalHomeRefreshStateRepository {

    Optional<PortalHomeRefreshState> findCurrent(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType
    );

    PortalHomeRefreshState save(
            PortalHomeRefreshStateScope scope,
            PortalHomeRefreshState refreshState
    );
}
