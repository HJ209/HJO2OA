package com.hjo2oa.portal.portal.home.infrastructure;

import com.hjo2oa.portal.aggregation.api.application.PortalDashboardAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AggregationApiPortalHomeAggregationViewProvider implements PortalHomeAggregationViewProvider {

    private final PortalDashboardAggregationApplicationService aggregationApplicationService;

    public AggregationApiPortalHomeAggregationViewProvider(
            PortalDashboardAggregationApplicationService aggregationApplicationService
    ) {
        this.aggregationApplicationService = aggregationApplicationService;
    }

    @Override
    public PortalDashboardView dashboard(PortalHomeSceneType sceneType, Set<PortalCardType> cardTypes) {
        return aggregationApplicationService.dashboard(mapSceneType(sceneType), cardTypes);
    }

    private PortalSceneType mapSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalSceneType.HOME;
            case OFFICE_CENTER -> PortalSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalSceneType.MOBILE_WORKBENCH;
        };
    }
}
