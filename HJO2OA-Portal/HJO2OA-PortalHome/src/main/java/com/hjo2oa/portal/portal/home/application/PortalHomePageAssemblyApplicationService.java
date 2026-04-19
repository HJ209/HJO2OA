package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardView;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalHomePageAssemblyApplicationService {

    private static final String SNAPSHOT_UNAVAILABLE_MESSAGE = "Card snapshot is unavailable";

    private final PortalHomePageTemplateProvider pageTemplateProvider;
    private final PortalHomeAggregationViewProvider aggregationViewProvider;
    private final Clock clock;

    public PortalHomePageAssemblyApplicationService(
            PortalHomePageTemplateProvider pageTemplateProvider,
            PortalHomeAggregationViewProvider aggregationViewProvider
    ) {
        this(pageTemplateProvider, aggregationViewProvider, Clock.systemUTC());
    }

    public PortalHomePageAssemblyApplicationService(
            PortalHomePageTemplateProvider pageTemplateProvider,
            PortalHomeAggregationViewProvider aggregationViewProvider,
            Clock clock
    ) {
        this.pageTemplateProvider = Objects.requireNonNull(pageTemplateProvider, "pageTemplateProvider must not be null");
        this.aggregationViewProvider = Objects.requireNonNull(aggregationViewProvider, "aggregationViewProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalHomePageView page(PortalHomeSceneType sceneType) {
        PortalHomeSceneType resolvedSceneType = sceneType == null ? PortalHomeSceneType.HOME : sceneType;
        PortalHomePageTemplate template = pageTemplateProvider.templateFor(resolvedSceneType);
        if (template == null) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Portal page template is unavailable");
        }

        PortalDashboardView dashboard = aggregationViewProvider.dashboard(resolvedSceneType, template.requestedCardTypes());
        Map<PortalCardType, PortalCardSnapshot<?>> snapshots = indexSnapshots(dashboard);

        List<PortalHomeRegionView> regions = template.regions().stream()
                .map(region -> toRegionView(region, snapshots))
                .toList();

        return new PortalHomePageView(
                template.sceneType(),
                template.layoutType(),
                template.branding(),
                template.navigation(),
                regions,
                template.footer(),
                clock.instant()
        );
    }

    private PortalHomeRegionView toRegionView(
            PortalHomeRegionTemplate regionTemplate,
            Map<PortalCardType, PortalCardSnapshot<?>> snapshots
    ) {
        List<PortalHomeCardView> cards = regionTemplate.cards().stream()
                .map(cardTemplate -> toCardView(cardTemplate, snapshots.get(cardTemplate.cardType())))
                .toList();

        return new PortalHomeRegionView(
                regionTemplate.regionCode(),
                regionTemplate.title(),
                regionTemplate.description(),
                cards
        );
    }

    private PortalHomeCardView toCardView(
            PortalHomeCardTemplate cardTemplate,
            PortalCardSnapshot<?> snapshot
    ) {
        if (snapshot == null) {
            return new PortalHomeCardView(
                    cardTemplate.cardCode(),
                    cardTemplate.cardType(),
                    cardTemplate.title(),
                    cardTemplate.description(),
                    cardTemplate.actionLink(),
                    PortalCardState.FAILED,
                    SNAPSHOT_UNAVAILABLE_MESSAGE,
                    null
            );
        }
        return new PortalHomeCardView(
                cardTemplate.cardCode(),
                cardTemplate.cardType(),
                cardTemplate.title(),
                cardTemplate.description(),
                cardTemplate.actionLink(),
                snapshot.state(),
                snapshot.message(),
                snapshot.data()
        );
    }

    private Map<PortalCardType, PortalCardSnapshot<?>> indexSnapshots(PortalDashboardView dashboard) {
        Map<PortalCardType, PortalCardSnapshot<?>> snapshots = new EnumMap<>(PortalCardType.class);
        snapshots.put(PortalCardType.IDENTITY, dashboard.identity());
        if (dashboard.todo() != null) {
            snapshots.put(PortalCardType.TODO, dashboard.todo());
        }
        if (dashboard.message() != null) {
            snapshots.put(PortalCardType.MESSAGE, dashboard.message());
        }
        return snapshots;
    }
}
