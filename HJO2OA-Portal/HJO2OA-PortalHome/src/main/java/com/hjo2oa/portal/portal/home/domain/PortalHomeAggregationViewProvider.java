package com.hjo2oa.portal.portal.home.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import java.util.Set;

public interface PortalHomeAggregationViewProvider {

    PortalDashboardView dashboard(PortalHomeSceneType sceneType, Set<PortalCardType> cardTypes);

    default PortalOfficeCenterView officeCenter() {
        return null;
    }
}
