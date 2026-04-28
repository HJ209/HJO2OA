package com.hjo2oa.portal.portal.home.infrastructure;

import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeBranding;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeFooter;
import com.hjo2oa.portal.portal.home.domain.PortalHomeLayoutType;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSourceTemplateMetadata;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegionView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalActiveTemplateResolutionView;
import com.hjo2oa.portal.portal.model.domain.PortalPageView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacementView;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class PortalModelBackedPortalHomePageTemplateProvider implements PortalHomePageTemplateProvider {

    private final PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService;
    private final PortalTemplateCanvasApplicationService templateCanvasApplicationService;
    private final StaticPortalHomePageTemplateProvider fallbackTemplateProvider;
    private final PersonalizationIdentityContextProvider identityContextProvider;
    @Autowired
    public PortalModelBackedPortalHomePageTemplateProvider(
            PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            PersonalizationIdentityContextProvider identityContextProvider
    ) {
        this(
                activeTemplateResolutionApplicationService,
                templateCanvasApplicationService,
                new StaticPortalHomePageTemplateProvider(),
                identityContextProvider
        );
    }

    public PortalModelBackedPortalHomePageTemplateProvider(
            PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            StaticPortalHomePageTemplateProvider fallbackTemplateProvider,
            PersonalizationIdentityContextProvider identityContextProvider
    ) {
        this.activeTemplateResolutionApplicationService = Objects.requireNonNull(
                activeTemplateResolutionApplicationService,
                "activeTemplateResolutionApplicationService must not be null"
        );
        this.templateCanvasApplicationService = Objects.requireNonNull(
                templateCanvasApplicationService,
                "templateCanvasApplicationService must not be null"
        );
        this.fallbackTemplateProvider = Objects.requireNonNull(
                fallbackTemplateProvider,
                "fallbackTemplateProvider must not be null"
        );
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
    }

    @Override
    public PortalHomePageTemplate templateFor(PortalHomeSceneType sceneType) {
        PortalHomePageTemplate fallbackTemplate = fallbackTemplateProvider.templateFor(sceneType);
        if (fallbackTemplate == null) {
            return null;
        }
        Optional<PortalActiveTemplateResolutionView> resolution = resolveActiveTemplate(sceneType);
        if (resolution.isEmpty()) {
            return fallbackTemplate;
        }
        PortalActiveTemplateResolutionView resolvedTemplate = resolution.orElseThrow();
        Optional<PortalTemplateCanvasView> canvas = templateCanvasApplicationService.currentPublished(
                resolvedTemplate.templateId()
        );
        if (canvas.isEmpty()) {
            return mergeTemplateMetadata(fallbackTemplate, resolvedTemplate);
        }
        return mergeTemplateStructure(
                fallbackTemplate,
                resolvedTemplate,
                canvas.orElseThrow()
        ).orElseGet(() -> mergeTemplateMetadata(fallbackTemplate, resolvedTemplate));
    }

    private Optional<PortalActiveTemplateResolutionView> resolveActiveTemplate(PortalHomeSceneType sceneType) {
        PortalPublicationSceneType publicationSceneType = mapSceneType(sceneType);
        PortalPublicationIdentity identity = currentIdentity();
        for (PortalPublicationClientType clientType : preferredClientTypes(sceneType)) {
            Optional<PortalActiveTemplateResolutionView> resolution =
                    activeTemplateResolutionApplicationService.currentActive(
                            publicationSceneType,
                            clientType,
                            identity
                    );
            if (resolution.isPresent()) {
                return resolution;
            }
        }
        return Optional.empty();
    }

    private PortalPublicationIdentity currentIdentity() {
        PersonalizationIdentityContext context = identityContextProvider.currentContext();
        return new PortalPublicationIdentity(
                context.assignmentId(),
                context.positionId(),
                context.personId()
        );
    }

    private PortalHomePageTemplate mergeTemplateMetadata(
            PortalHomePageTemplate fallbackTemplate,
            PortalActiveTemplateResolutionView resolution
    ) {
        return new PortalHomePageTemplate(
                fallbackTemplate.sceneType(),
                fallbackTemplate.layoutType(),
                new PortalHomeBranding(
                        resolution.templateDisplayName(),
                        fallbackTemplate.branding().subtitle(),
                        fallbackTemplate.branding().logoText()
                ),
                fallbackTemplate.navigation(),
                fallbackTemplate.regions(),
                new PortalHomeFooter(
                        "Resolved from template " + resolution.templateCode()
                                + " via publication " + resolution.publicationId()
                ),
                PortalHomeSourceTemplateMetadata.from(resolution)
        );
    }

    private Optional<PortalHomePageTemplate> mergeTemplateStructure(
            PortalHomePageTemplate fallbackTemplate,
            PortalActiveTemplateResolutionView resolution,
            PortalTemplateCanvasView canvas
    ) {
        PortalPageView page = resolvePage(canvas).orElse(null);
        if (page == null || page.regions().isEmpty()) {
            return Optional.empty();
        }

        Map<PortalCardType, PortalHomeCardTemplate> fallbackCards = fallbackCardCatalog(fallbackTemplate);
        Map<String, PortalHomeRegionTemplate> fallbackRegions = fallbackRegionCatalog(fallbackTemplate);
        List<PortalHomeRegionTemplate> regions = page.regions().stream()
                .map(region -> toRegionTemplate(region, fallbackCards, fallbackRegions))
                .filter(Objects::nonNull)
                .toList();
        if (regions.isEmpty() || regions.stream().allMatch(region -> region.cards().isEmpty())) {
            return Optional.empty();
        }

        return Optional.of(new PortalHomePageTemplate(
                fallbackTemplate.sceneType(),
                mapLayoutType(page.layoutMode()),
                new PortalHomeBranding(
                        resolution.templateDisplayName(),
                        fallbackTemplate.branding().subtitle(),
                        fallbackTemplate.branding().logoText()
                ),
                fallbackTemplate.navigation(),
                regions,
                new PortalHomeFooter(
                        "Resolved from template " + resolution.templateCode()
                                + " page " + page.pageCode()
                                + " via publication " + resolution.publicationId()
                ),
                PortalHomeSourceTemplateMetadata.from(resolution)
        ));
    }

    private Optional<PortalPageView> resolvePage(PortalTemplateCanvasView canvas) {
        return canvas.pages().stream()
                .filter(PortalPageView::defaultPage)
                .findFirst()
                .or(() -> canvas.pages().stream().findFirst());
    }

    private PortalHomeRegionTemplate toRegionTemplate(
            PortalLayoutRegionView region,
            Map<PortalCardType, PortalHomeCardTemplate> fallbackCards,
            Map<String, PortalHomeRegionTemplate> fallbackRegions
    ) {
        List<PortalHomeCardTemplate> cards = region.placements().stream()
                .sorted(Comparator.comparingInt(PortalWidgetPlacementView::orderNo))
                .map(placement -> toCardTemplate(placement, fallbackCards))
                .filter(Objects::nonNull)
                .toList();
        PortalHomeRegionTemplate fallbackRegion = fallbackRegions.get(region.regionCode());
        return new PortalHomeRegionTemplate(
                region.regionCode(),
                region.title(),
                fallbackRegion != null
                        ? fallbackRegion.description()
                        : resolveRegionDescription(region, cards),
                cards
        );
    }

    private PortalHomeCardTemplate toCardTemplate(
            PortalWidgetPlacementView placement,
            Map<PortalCardType, PortalHomeCardTemplate> fallbackCards
    ) {
        PortalCardType cardType = mapCardType(placement.cardType());
        PortalHomeCardTemplate fallbackCard = fallbackCards.get(cardType);
        if (fallbackCard == null) {
            return null;
        }
        return new PortalHomeCardTemplate(
                fallbackCard.cardCode(),
                cardType,
                fallbackCard.title(),
                fallbackCard.description(),
                fallbackCard.actionLink(),
                placement.placementCode(),
                placement.widgetCode()
        );
    }

    private String resolveRegionDescription(
            PortalLayoutRegionView region,
            List<PortalHomeCardTemplate> cards
    ) {
        if (!cards.isEmpty()) {
            return cards.size() == 1
                    ? "Source canvas region for " + cards.get(0).title()
                    : "Source canvas region assembled from template placements";
        }
        return "Source canvas region " + region.regionCode();
    }

    private Map<PortalCardType, PortalHomeCardTemplate> fallbackCardCatalog(PortalHomePageTemplate fallbackTemplate) {
        Map<PortalCardType, PortalHomeCardTemplate> cards = new EnumMap<>(PortalCardType.class);
        fallbackTemplate.regions().stream()
                .flatMap(region -> region.cards().stream())
                .forEach(card -> cards.putIfAbsent(card.cardType(), card));
        return cards;
    }

    private Map<String, PortalHomeRegionTemplate> fallbackRegionCatalog(PortalHomePageTemplate fallbackTemplate) {
        return fallbackTemplate.regions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PortalHomeRegionTemplate::regionCode,
                        region -> region
                ));
    }

    private PortalHomeLayoutType mapLayoutType(PortalTemplateLayoutMode layoutMode) {
        return switch (layoutMode) {
            case THREE_SECTION -> PortalHomeLayoutType.THREE_SECTION;
            case OFFICE_SPLIT -> PortalHomeLayoutType.OFFICE_SPLIT;
            case MOBILE_LIGHT -> PortalHomeLayoutType.MOBILE_LIGHT;
        };
    }

    private PortalCardType mapCardType(WidgetCardType cardType) {
        return switch (cardType) {
            case IDENTITY -> PortalCardType.IDENTITY;
            case TODO -> PortalCardType.TODO;
            case MESSAGE -> PortalCardType.MESSAGE;
        };
    }

    private PortalPublicationSceneType mapSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalPublicationSceneType.HOME;
            case OFFICE_CENTER -> PortalPublicationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalPublicationSceneType.MOBILE_WORKBENCH;
        };
    }

    private List<PortalPublicationClientType> preferredClientTypes(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> List.of(PortalPublicationClientType.PC, PortalPublicationClientType.ALL);
            case OFFICE_CENTER -> List.of(PortalPublicationClientType.PC, PortalPublicationClientType.ALL);
            case MOBILE_WORKBENCH -> List.of(PortalPublicationClientType.MOBILE, PortalPublicationClientType.ALL);
        };
    }
}
