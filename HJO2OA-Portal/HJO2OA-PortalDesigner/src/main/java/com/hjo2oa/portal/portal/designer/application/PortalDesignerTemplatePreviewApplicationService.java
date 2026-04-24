package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileView;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerPreviewIdentityContext;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerPreviewIdentityView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerPreviewOverlayView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePreviewView;
import com.hjo2oa.portal.portal.home.application.PortalHomeOverlayApplicationResult;
import com.hjo2oa.portal.portal.home.application.PortalHomeOverlayApplicator;
import com.hjo2oa.portal.portal.home.application.PortalHomePageAssemblyApplicationService;
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlay;
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlayProvider;
import com.hjo2oa.portal.portal.home.application.PortalHomeRefreshStateApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeBranding;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeFooter;
import com.hjo2oa.portal.portal.home.domain.PortalHomeLayoutType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeNavigationItem;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.infrastructure.InMemoryPortalHomeRefreshStateRepository;
import com.hjo2oa.portal.portal.home.infrastructure.StaticPortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegionView;
import com.hjo2oa.portal.portal.model.domain.PortalPageView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacementView;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplatePreviewApplicationService {

    private final PortalTemplateApplicationService templateApplicationService;
    private final PortalTemplateCanvasApplicationService templateCanvasApplicationService;
    private final Clock clock;
    private final PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService;
    private final PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService;
    private final PersonalizationProfileApplicationService personalizationProfileApplicationService;
    private final PortalHomeOverlayApplicator overlayApplicator;

    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService
    ) {
        this(
                templateApplicationService,
                templateCanvasApplicationService,
                Clock.systemUTC(),
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this(
                templateApplicationService,
                templateCanvasApplicationService,
                Clock.systemUTC(),
                widgetReferenceStatusApplicationService
        );
    }

    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            Clock clock
    ) {
        this(
                templateApplicationService,
                templateCanvasApplicationService,
                clock,
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            Clock clock,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this(
                templateApplicationService,
                templateCanvasApplicationService,
                clock,
                widgetReferenceStatusApplicationService,
                defaultActiveTemplateResolutionApplicationService(templateApplicationService, clock),
                defaultPersonalizationProfileApplicationService(clock)
        );
    }

    @Autowired
    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService,
            PersonalizationProfileApplicationService personalizationProfileApplicationService
    ) {
        this(
                templateApplicationService,
                templateCanvasApplicationService,
                Clock.systemUTC(),
                widgetReferenceStatusApplicationService,
                activeTemplateResolutionApplicationService,
                personalizationProfileApplicationService
        );
    }

    public PortalDesignerTemplatePreviewApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            Clock clock,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService,
            PersonalizationProfileApplicationService personalizationProfileApplicationService
    ) {
        this.templateApplicationService = Objects.requireNonNull(
                templateApplicationService,
                "templateApplicationService must not be null"
        );
        this.templateCanvasApplicationService = Objects.requireNonNull(
                templateCanvasApplicationService,
                "templateCanvasApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.widgetReferenceStatusApplicationService = Objects.requireNonNull(
                widgetReferenceStatusApplicationService,
                "widgetReferenceStatusApplicationService must not be null"
        );
        this.activeTemplateResolutionApplicationService = Objects.requireNonNull(
                activeTemplateResolutionApplicationService,
                "activeTemplateResolutionApplicationService must not be null"
        );
        this.personalizationProfileApplicationService = Objects.requireNonNull(
                personalizationProfileApplicationService,
                "personalizationProfileApplicationService must not be null"
        );
        this.overlayApplicator = new PortalHomeOverlayApplicator();
    }

    public PortalDesignerTemplatePreviewView preview(
            String templateId,
            PortalPublicationClientType clientType,
            PortalDesignerPreviewIdentityContext previewIdentityContext
    ) {
        Objects.requireNonNull(templateId, "templateId must not be null");

        PortalTemplateView template = templateApplicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        PortalTemplateCanvasView canvas = templateCanvasApplicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template canvas not found"
                ));
        widgetReferenceStatusApplicationService.ensureNoRepairRequiredReferences(template.tenantId(), canvas.pages());

        PortalHomeSceneType homeSceneType = mapSceneType(template.sceneType());
        PortalPublicationClientType resolvedClientType = clientType == null
                ? defaultClientType(homeSceneType)
                : clientType;
        PortalIdentityCard previewIdentity = sampleIdentityCard(
                resolvedClientType,
                previewIdentityContext,
                clock.instant()
        );
        PortalHomePageTemplate pageTemplate = buildPreviewTemplate(homeSceneType, template, canvas);
        PortalHomeOverlayApplicationResult overlayResult = overlayApplicator.apply(
                pageTemplate,
                resolveLivePublicationId(homeSceneType, resolvedClientType, previewIdentity),
                resolveOverlay(homeSceneType, previewIdentity)
        );
        PortalHomePageView pageView = new PortalHomePageAssemblyApplicationService(
                previewTemplateProvider(homeSceneType, overlayResult.template()),
                previewAggregationViewProvider(previewIdentity),
                new PortalHomeRefreshStateApplicationService(
                        new InMemoryPortalHomeRefreshStateRepository(),
                        () -> new PersonalizationIdentityContext(
                                previewIdentity.tenantId(),
                                previewIdentity.personId(),
                                previewIdentity.assignmentId(),
                                previewIdentity.positionId()
                        ),
                        clock
                ),
                noOverlayProvider()
        ).page(homeSceneType);

        return new PortalDesignerTemplatePreviewView(
                template.templateId(),
                template.templateCode(),
                template.displayName(),
                template.sceneType(),
                resolvedClientType,
                template.latestVersionNo(),
                template.publishedVersionNo(),
                PortalDesignerPreviewIdentityView.from(previewIdentity),
                PortalDesignerPreviewOverlayView.from(overlayResult),
                pageView,
                clock.instant()
        );
    }

    private PortalHomePageTemplateProvider previewTemplateProvider(
            PortalHomeSceneType expectedSceneType,
            PortalHomePageTemplate pageTemplate
    ) {
        return requestedSceneType -> requestedSceneType == expectedSceneType ? pageTemplate : null;
    }

    private PortalHomeAggregationViewProvider previewAggregationViewProvider(PortalIdentityCard identityCard) {
        return (sceneType, cardTypes) -> previewDashboard(sceneType, identityCard, cardTypes);
    }

    private PortalHomePersonalizationOverlayProvider noOverlayProvider() {
        return sceneType -> PortalHomePersonalizationOverlay.none();
    }

    private PortalDashboardView previewDashboard(
            PortalHomeSceneType sceneType,
            PortalIdentityCard identityCard,
            Set<PortalCardType> cardTypes
    ) {
        Instant now = clock.instant();
        PortalSceneType aggregationSceneType = mapAggregationSceneType(sceneType);
        PortalCardSnapshot<PortalIdentityCard> identitySnapshot = PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identityCard, aggregationSceneType, PortalCardType.IDENTITY),
                PortalCardType.IDENTITY,
                identityCard,
                now
        );

        PortalCardSnapshot<PortalTodoCard> todoSnapshot = cardTypes.contains(PortalCardType.TODO)
                ? PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identityCard, aggregationSceneType, PortalCardType.TODO),
                PortalCardType.TODO,
                PortalTodoCard.empty(),
                now
        )
                : null;
        PortalCardSnapshot<PortalMessageCard> messageSnapshot = cardTypes.contains(PortalCardType.MESSAGE)
                ? PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identityCard, aggregationSceneType, PortalCardType.MESSAGE),
                PortalCardType.MESSAGE,
                PortalMessageCard.empty(),
                now
        )
                : null;

        return new PortalDashboardView(
                aggregationSceneType,
                identitySnapshot,
                todoSnapshot,
                messageSnapshot
        );
    }

    private PortalIdentityCard sampleIdentityCard(
            PortalPublicationClientType clientType,
            PortalDesignerPreviewIdentityContext previewIdentityContext,
            Instant effectiveAt
    ) {
        String tenantId = previewIdentityContext == null || previewIdentityContext.tenantId() == null
                ? "preview-tenant"
                : previewIdentityContext.tenantId();
        String personId = previewIdentityContext == null || previewIdentityContext.personId() == null
                ? "preview-person"
                : previewIdentityContext.personId();
        String accountId = previewIdentityContext == null || previewIdentityContext.accountId() == null
                ? "preview-account"
                : previewIdentityContext.accountId();
        String assignmentId = previewIdentityContext == null || previewIdentityContext.assignmentId() == null
                ? "preview-assignment-" + clientType.name().toLowerCase()
                : previewIdentityContext.assignmentId();
        String positionId = previewIdentityContext == null || previewIdentityContext.positionId() == null
                ? "preview-position"
                : previewIdentityContext.positionId();
        return new PortalIdentityCard(
                tenantId,
                personId,
                accountId,
                assignmentId,
                positionId,
                "preview-organization",
                "preview-department",
                "Preview " + clientType.name() + " Position",
                "Preview Organization",
                "Preview Department",
                "PRIMARY",
                effectiveAt
        );
    }

    private PortalHomePageTemplate buildPreviewTemplate(
            PortalHomeSceneType homeSceneType,
            PortalTemplateView template,
            PortalTemplateCanvasView canvas
    ) {
        PortalHomePageTemplate fallbackTemplate = new StaticPortalHomePageTemplateProvider().templateFor(homeSceneType);
        if (fallbackTemplate == null) {
            throw new BizException(
                    SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                    "Preview page template is unavailable"
            );
        }
        return mergeTemplateStructure(fallbackTemplate, template, canvas)
                .orElseGet(() -> mergeTemplateMetadata(fallbackTemplate, template));
    }

    private PortalHomePageTemplate mergeTemplateMetadata(
            PortalHomePageTemplate fallbackTemplate,
            PortalTemplateView template
    ) {
        return new PortalHomePageTemplate(
                fallbackTemplate.sceneType(),
                fallbackTemplate.layoutType(),
                new PortalHomeBranding(
                        template.displayName(),
                        fallbackTemplate.branding().subtitle(),
                        fallbackTemplate.branding().logoText()
                ),
                fallbackTemplate.navigation(),
                fallbackTemplate.regions(),
                new PortalHomeFooter("Preview assembled from draft template " + template.templateCode())
        );
    }

    private Optional<PortalHomePageTemplate> mergeTemplateStructure(
            PortalHomePageTemplate fallbackTemplate,
            PortalTemplateView template,
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
                        template.displayName(),
                        fallbackTemplate.branding().subtitle(),
                        fallbackTemplate.branding().logoText()
                ),
                fallbackTemplate.navigation(),
                regions,
                new PortalHomeFooter(
                        "Preview assembled from draft template " + template.templateCode()
                                + " page " + page.pageCode()
                )
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

    private PortalHomePersonalizationOverlay resolveOverlay(
            PortalHomeSceneType homeSceneType,
            PortalIdentityCard previewIdentity
    ) {
        PersonalizationProfileView profile = personalizationProfileApplicationService.current(
                mapPersonalizationSceneType(homeSceneType),
                new PersonalizationIdentityContext(
                        previewIdentity.tenantId(),
                        previewIdentity.personId(),
                        previewIdentity.assignmentId(),
                        previewIdentity.positionId()
                )
        );
        return new PortalHomePersonalizationOverlay(
                profile.basePublicationId(),
                profile.widgetOrderOverride(),
                profile.hiddenPlacementCodes()
        );
    }

    private String resolveLivePublicationId(
            PortalHomeSceneType homeSceneType,
            PortalPublicationClientType clientType,
            PortalIdentityCard previewIdentity
    ) {
        PortalPublicationIdentity identity = new PortalPublicationIdentity(
                previewIdentity.assignmentId(),
                previewIdentity.positionId(),
                previewIdentity.personId()
        );
        for (PortalPublicationClientType candidateClientType : preferredLiveClientTypes(homeSceneType, clientType)) {
            Optional<String> publicationId = activeTemplateResolutionApplicationService.currentActive(
                    mapPublicationSceneType(homeSceneType),
                    candidateClientType,
                    identity
            ).map(resolution -> resolution.publicationId());
            if (publicationId.isPresent()) {
                return publicationId.orElseThrow();
            }
        }
        return null;
    }

    private String resolveRegionDescription(
            PortalLayoutRegionView region,
            List<PortalHomeCardTemplate> cards
    ) {
        if (!cards.isEmpty()) {
            return cards.size() == 1
                    ? "Preview region for " + cards.get(0).title()
                    : "Preview region assembled from draft placements";
        }
        return "Preview region " + region.regionCode();
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

    private PortalPublicationClientType defaultClientType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME, OFFICE_CENTER -> PortalPublicationClientType.PC;
            case MOBILE_WORKBENCH -> PortalPublicationClientType.MOBILE;
        };
    }

    private List<PortalPublicationClientType> preferredLiveClientTypes(
            PortalHomeSceneType sceneType,
            PortalPublicationClientType clientType
    ) {
        if (clientType == PortalPublicationClientType.ALL) {
            return List.of(PortalPublicationClientType.ALL);
        }
        return switch (sceneType) {
            case HOME, OFFICE_CENTER -> clientType == PortalPublicationClientType.PC
                    ? List.of(PortalPublicationClientType.PC, PortalPublicationClientType.ALL)
                    : List.of(clientType, PortalPublicationClientType.ALL);
            case MOBILE_WORKBENCH -> clientType == PortalPublicationClientType.MOBILE
                    ? List.of(PortalPublicationClientType.MOBILE, PortalPublicationClientType.ALL)
                    : List.of(clientType, PortalPublicationClientType.ALL);
        };
    }

    private PortalHomeSceneType mapSceneType(PortalPublicationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalHomeSceneType.HOME;
            case OFFICE_CENTER -> PortalHomeSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalHomeSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalPublicationSceneType mapPublicationSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalPublicationSceneType.HOME;
            case OFFICE_CENTER -> PortalPublicationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalPublicationSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalSceneType mapAggregationSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalSceneType.HOME;
            case OFFICE_CENTER -> PortalSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalSceneType.MOBILE_WORKBENCH;
        };
    }

    private PersonalizationSceneType mapPersonalizationSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PersonalizationSceneType.HOME;
            case OFFICE_CENTER -> PersonalizationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PersonalizationSceneType.MOBILE_WORKBENCH;
        };
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

    private static PortalActiveTemplateResolutionApplicationService defaultActiveTemplateResolutionApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            Clock clock
    ) {
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                () -> new PortalModelContext("preview-tenant", "portal-designer-preview"),
                event -> {
                },
                clock
        );
        return new PortalActiveTemplateResolutionApplicationService(
                publicationApplicationService,
                templateApplicationService
        );
    }

    private static PersonalizationProfileApplicationService defaultPersonalizationProfileApplicationService(Clock clock) {
        return new PersonalizationProfileApplicationService(
                new com.hjo2oa.portal.personalization.infrastructure.InMemoryPersonalizationProfileRepository(),
                () -> new PersonalizationIdentityContext(
                        "preview-tenant",
                        "preview-person",
                        "preview-assignment",
                        "preview-position"
                ),
                new com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver(),
                event -> {
                },
                clock
        );
    }
}
