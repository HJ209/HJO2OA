package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterNavItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeNavigationItem;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshState;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class PortalHomePageAssemblyApplicationService {

    private static final String SNAPSHOT_UNAVAILABLE_MESSAGE = "Card snapshot is unavailable";

    private final PortalHomePageTemplateProvider pageTemplateProvider;
    private final PortalHomeAggregationViewProvider aggregationViewProvider;
    private final PortalHomeRefreshStateApplicationService refreshStateApplicationService;
    private final PortalHomePersonalizationOverlayProvider personalizationOverlayProvider;
    private final PortalHomeOverlayApplicator overlayApplicator;
    private final Clock clock;

    public PortalHomePageAssemblyApplicationService(
            PortalHomePageTemplateProvider pageTemplateProvider,
            PortalHomeAggregationViewProvider aggregationViewProvider,
            PortalHomeRefreshStateApplicationService refreshStateApplicationService,
            PortalHomePersonalizationOverlayProvider personalizationOverlayProvider
    ) {
        this(
                pageTemplateProvider,
                aggregationViewProvider,
                refreshStateApplicationService,
                personalizationOverlayProvider,
                Clock.systemUTC()
        );
    }

    PortalHomePageAssemblyApplicationService(
            PortalHomePageTemplateProvider pageTemplateProvider,
            PortalHomeAggregationViewProvider aggregationViewProvider,
            PortalHomeRefreshStateApplicationService refreshStateApplicationService,
            PortalHomePersonalizationOverlayProvider personalizationOverlayProvider,
            Clock clock
    ) {
        this.pageTemplateProvider = Objects.requireNonNull(pageTemplateProvider, "pageTemplateProvider must not be null");
        this.aggregationViewProvider = Objects.requireNonNull(aggregationViewProvider, "aggregationViewProvider must not be null");
        this.refreshStateApplicationService = Objects.requireNonNull(
                refreshStateApplicationService,
                "refreshStateApplicationService must not be null"
        );
        this.personalizationOverlayProvider = Objects.requireNonNull(
                personalizationOverlayProvider,
                "personalizationOverlayProvider must not be null"
        );
        this.overlayApplicator = new PortalHomeOverlayApplicator();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalHomePageView page(PortalHomeSceneType sceneType) {
        PortalHomeSceneType resolvedSceneType = sceneType == null ? PortalHomeSceneType.HOME : sceneType;
        PortalHomePageTemplate template = pageTemplateProvider.templateFor(resolvedSceneType);
        if (template == null) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Portal page template is unavailable");
        }
        PortalHomePageTemplate effectiveTemplate = applyOverlayIfEligible(resolvedSceneType, template);

        PortalOfficeCenterView officeCenter = resolvedSceneType == PortalHomeSceneType.OFFICE_CENTER
                ? aggregationViewProvider.officeCenter()
                : null;
        PortalDashboardView dashboard = resolveDashboard(
                resolvedSceneType,
                effectiveTemplate.requestedCardTypes(),
                officeCenter
        );
        Map<PortalCardType, PortalCardSnapshot<?>> snapshots = indexSnapshots(dashboard);
        List<PortalHomeNavigationItem> navigation = resolveNavigation(
                resolvedSceneType,
                effectiveTemplate,
                officeCenter,
                dashboard
        );
        PortalHomeRefreshState refreshState = refreshStateApplicationService.currentState(resolvedSceneType);

        List<PortalHomeRegionView> regions = effectiveTemplate.regions().stream()
                .map(region -> toRegionView(region, snapshots))
                .toList();

        return new PortalHomePageView(
                effectiveTemplate.sceneType(),
                effectiveTemplate.layoutType(),
                effectiveTemplate.branding(),
                navigation,
                regions,
                effectiveTemplate.footer(),
                refreshState,
                effectiveTemplate.sourceTemplateMetadata(),
                clock.instant()
        );
    }

    private PortalHomePageTemplate applyOverlayIfEligible(
            PortalHomeSceneType sceneType,
            PortalHomePageTemplate template
    ) {
        if (template.sourceTemplateMetadata() == null) {
            return template;
        }
        PortalHomeOverlayApplicationResult overlayResult = overlayApplicator.apply(
                template,
                template.sourceTemplateMetadata().publicationId(),
                personalizationOverlayProvider.currentOverlay(sceneType)
        );
        return overlayResult.template();
    }

    private PortalDashboardView resolveDashboard(
            PortalHomeSceneType sceneType,
            java.util.Set<PortalCardType> requestedCardTypes,
            PortalOfficeCenterView officeCenter
    ) {
        if (sceneType == PortalHomeSceneType.OFFICE_CENTER && officeCenter != null) {
            return new PortalDashboardView(
                    PortalSceneType.OFFICE_CENTER,
                    officeCenter.identity(),
                    officeCenter.todo(),
                    officeCenter.message()
            );
        }
        return aggregationViewProvider.dashboard(sceneType, requestedCardTypes);
    }

    private List<PortalHomeNavigationItem> resolveNavigation(
            PortalHomeSceneType sceneType,
            PortalHomePageTemplate template,
            PortalOfficeCenterView officeCenter,
            PortalDashboardView dashboard
    ) {
        if (sceneType == PortalHomeSceneType.OFFICE_CENTER) {
            if (officeCenter == null || officeCenter.navigation().isEmpty()) {
                return template.navigation();
            }
            return officeCenter.navigation().stream()
                    .map(this::toNavigationItem)
                    .toList();
        }
        if (sceneType == PortalHomeSceneType.MOBILE_WORKBENCH) {
            return template.navigation().stream()
                    .map(item -> toMobileNavigationItem(item, dashboard))
                    .toList();
        }
        return template.navigation();
    }

    private PortalHomeNavigationItem toNavigationItem(PortalOfficeCenterNavItem navItem) {
        return new PortalHomeNavigationItem(
                navItem.code(),
                navItem.title(),
                navItem.badgeCount(),
                navItem.actionLink()
        );
    }

    private PortalHomeNavigationItem toMobileNavigationItem(
            PortalHomeNavigationItem item,
            PortalDashboardView dashboard
    ) {
        return switch (item.code()) {
            case "tasks" -> new PortalHomeNavigationItem(
                    item.code(),
                    item.title(),
                    resolveTodoBadge(dashboard),
                    item.actionLink()
            );
            case "messages" -> new PortalHomeNavigationItem(
                    item.code(),
                    item.title(),
                    resolveMessageBadge(dashboard),
                    item.actionLink()
            );
            default -> item;
        };
    }

    private Long resolveTodoBadge(PortalDashboardView dashboard) {
        if (dashboard.todo() == null || dashboard.todo().data() == null) {
            return null;
        }
        PortalTodoCard todoCard = dashboard.todo().data();
        return todoCard.totalCount();
    }

    private Long resolveMessageBadge(PortalDashboardView dashboard) {
        if (dashboard.message() == null || dashboard.message().data() == null) {
            return null;
        }
        PortalMessageCard messageCard = dashboard.message().data();
        return messageCard.unreadCount();
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
