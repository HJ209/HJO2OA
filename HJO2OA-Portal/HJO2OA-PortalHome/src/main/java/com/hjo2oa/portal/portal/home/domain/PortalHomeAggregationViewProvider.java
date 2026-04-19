package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import java.util.Set;

public interface PortalHomeAggregationViewProvider {

    PortalDashboardView dashboard(PortalHomeSceneType sceneType, Set<PortalCardType> cardTypes);
}
