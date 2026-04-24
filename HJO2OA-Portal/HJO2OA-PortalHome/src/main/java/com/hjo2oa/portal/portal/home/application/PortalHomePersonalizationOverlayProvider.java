package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;

@FunctionalInterface
public interface PortalHomePersonalizationOverlayProvider {

    PortalHomePersonalizationOverlay currentOverlay(PortalHomeSceneType sceneType);
}
