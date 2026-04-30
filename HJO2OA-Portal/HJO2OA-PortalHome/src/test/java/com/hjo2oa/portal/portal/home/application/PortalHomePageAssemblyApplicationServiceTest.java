package com.hjo2oa.portal.portal.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterNavItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSourceTemplateMetadata;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.infrastructure.InMemoryPortalHomeRefreshStateRepository;
import com.hjo2oa.portal.portal.home.infrastructure.PortalModelBackedPortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.infrastructure.StaticPortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PortalHomePageAssemblyApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T12:00:00Z");

    @Test
    void shouldAssembleHomePageWithThreeSectionLayout() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.layoutType().name()).isEqualTo("THREE_SECTION");
        assertThat(page.regions()).hasSize(2);
        assertThat(page.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("IDENTITY");
        assertThat(page.regions().get(1).cards()).extracting(card -> card.cardType().name())
                .containsExactly("TODO", "MESSAGE");
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO,
                PortalCardType.MESSAGE
        );
        assertThat(page.refreshState().status().name()).isEqualTo("IDLE");
        assertThat(page.sourceTemplateMetadata()).isNull();
    }

    @Test
    void shouldKeepDegradedCardInPageAssembly() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                (sceneType, cardTypes) -> dashboardWithDegradedMessage(),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.regions().get(1).cards()).anySatisfy(card -> {
            if (card.cardType() == PortalCardType.MESSAGE) {
                assertThat(card.state().name()).isEqualTo("FAILED");
                assertThat(card.message()).isEqualTo("Message card is temporarily unavailable");
            }
        });
    }

    @Test
    void shouldAssembleMobileWorkbenchWithMobileLayout() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                (sceneType, cardTypes) -> dashboardReady(),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.MOBILE_WORKBENCH);

        assertThat(page.layoutType().name()).isEqualTo("MOBILE_LIGHT");
        assertThat(page.navigation())
                .extracting(nav -> nav.code() + ":" + nav.badgeCount())
                .containsExactly("tasks:1", "messages:1");
        assertThat(page.regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("TODO", "MESSAGE", "IDENTITY"));
    }

    @Test
    void shouldAssembleOfficeCenterUsingDedicatedAggregationNavigation() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                officeCenterProvider(),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.OFFICE_CENTER);

        assertThat(page.layoutType().name()).isEqualTo("OFFICE_SPLIT");
        assertThat(page.navigation())
                .extracting(nav -> nav.code() + ":" + nav.badgeCount())
                .containsExactly("pending:2", "completed:1", "overdue:1", "messages:3");
        assertThat(page.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("IDENTITY");
        assertThat(page.regions().get(1).cards()).extracting(card -> card.cardType().name())
                .containsExactly("TODO", "MESSAGE");
    }

    @Test
    void shouldExposeSourceTemplateMetadataWhenTemplateProviderResolvedIt() {
        StaticPortalHomePageTemplateProvider staticProvider = new StaticPortalHomePageTemplateProvider();
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                sceneType -> new com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate(
                        staticProvider.templateFor(sceneType).sceneType(),
                        staticProvider.templateFor(sceneType).layoutType(),
                        staticProvider.templateFor(sceneType).branding(),
                        staticProvider.templateFor(sceneType).navigation(),
                        staticProvider.templateFor(sceneType).regions(),
                        staticProvider.templateFor(sceneType).footer(),
                        new PortalHomeSourceTemplateMetadata(
                                "publication-1",
                                "template-1",
                                "home-default",
                                "Home Default",
                                PortalPublicationSceneType.HOME,
                                PortalPublicationClientType.ALL,
                                PortalPublicationStatus.ACTIVE,
                                1,
                                1,
                                FIXED_TIME,
                                FIXED_TIME,
                                FIXED_TIME
                        )
                ),
                capturingProvider(new AtomicReference<>(), dashboardReady()),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.sourceTemplateMetadata()).isNotNull();
        assertThat(page.sourceTemplateMetadata().templateCode()).isEqualTo("home-default");
        assertThat(page.sourceTemplateMetadata().publicationId()).isEqualTo("publication-1");
    }

    @Test
    void shouldAssemblePageUsingSourceCanvasRegionAndCardOrder() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(templateRepository, "template-1", List.of(new PortalPage(
                "page-home-custom",
                "home-custom",
                "Home Custom",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(
                        new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                                )
                        ),
                        new PortalLayoutRegion(
                                "region-identity-overview",
                                "identity-overview",
                                "Identity Overview",
                                true,
                                List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30))
                        )
                )
        )));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                ),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.sourceTemplateMetadata()).isNotNull();
        assertThat(page.sourceTemplateMetadata().templateCode()).isEqualTo("home-default");
        assertThat(page.regions()).extracting(region -> region.regionCode())
                .containsExactly("work-focus", "identity-overview");
        assertThat(page.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("MESSAGE", "TODO");
        assertThat(page.regions().get(0).cards()).extracting(card -> card.title())
                .containsExactly("Unread Messages", "Pending Tasks");
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO,
                PortalCardType.MESSAGE
        );
    }

    @Test
    void shouldApplyHiddenPlacementsAndSameRegionWidgetOrderWhenPublicationMatchesProfile() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(templateRepository, "template-1", overlayPages());
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                ),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                overlayProvider(
                        "publication-1",
                        List.of("todo-card", "identity-card"),
                        List.of("placement-message")
                ),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.regions()).singleElement().satisfies(region -> {
            assertThat(region.regionCode()).isEqualTo("work-focus");
            assertThat(region.cards()).extracting(card -> card.cardType().name())
                    .containsExactly("TODO", "IDENTITY");
        });
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO
        );
    }

    @Test
    void shouldIgnoreOverlayWhenProfilePublicationDoesNotMatchLivePublication() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(templateRepository, "template-1", overlayPages());
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                ),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                overlayProvider(
                        "publication-outdated",
                        List.of("todo-card", "identity-card"),
                        List.of("placement-message")
                ),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("MESSAGE", "TODO", "IDENTITY"));
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO,
                PortalCardType.MESSAGE
        );
    }

    @Test
    void shouldIgnoreUnknownOverlayCodesAsStableNoOp() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(templateRepository, "template-1", overlayPages());
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                ),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                overlayProvider(
                        "publication-1",
                        List.of("widget-missing", "placement-missing"),
                        List.of("placement-missing")
                ),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("MESSAGE", "TODO", "IDENTITY"));
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO,
                PortalCardType.MESSAGE
        );
    }

    @Test
    void shouldKeepLivePageStableAfterPublishingAndContinuingDraftChanges() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(templateRepository, "template-1", publishedPages());
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        overrideTemplatePages(templateRepository, "template-1", draftPages());
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                ),
                capturingProvider(requestedCards, dashboardReady()),
                refreshStateApplicationService(),
                noOverlayProvider(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView publishedPage = service.page(PortalHomeSceneType.HOME);

        assertThat(publishedPage.regions()).extracting(region -> region.regionCode())
                .containsExactly("work-focus");
        assertThat(publishedPage.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("MESSAGE", "TODO");

        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 2));

        PortalHomePageView republishedPage = service.page(PortalHomeSceneType.HOME);

        assertThat(republishedPage.regions()).extracting(region -> region.regionCode())
                .containsExactly("identity-overview");
        assertThat(republishedPage.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("IDENTITY");
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(PortalCardType.IDENTITY);
    }

    private PortalHomeAggregationViewProvider capturingProvider(
            AtomicReference<Set<PortalCardType>> requestedCards,
            PortalDashboardView response
    ) {
        return (sceneType, cardTypes) -> {
            requestedCards.set(Set.copyOf(cardTypes));
            return response;
        };
    }

    private PortalHomeAggregationViewProvider officeCenterProvider() {
        return new PortalHomeAggregationViewProvider() {
            @Override
            public PortalDashboardView dashboard(PortalHomeSceneType sceneType, Set<PortalCardType> cardTypes) {
                return dashboardReady();
            }

            @Override
            public PortalOfficeCenterView officeCenter() {
                PortalDashboardView dashboard = officeCenterDashboardReady();
                return new PortalOfficeCenterView(
                        PortalSceneType.OFFICE_CENTER,
                        java.util.List.of(
                                new PortalOfficeCenterNavItem("pending", "Pending", 2, "/api/v1/todo/pending"),
                                new PortalOfficeCenterNavItem("completed", "Completed", 1, "/api/v1/todo/completed"),
                                new PortalOfficeCenterNavItem("overdue", "Overdue", 1, "/api/v1/todo/overdue"),
                                new PortalOfficeCenterNavItem(
                                        "messages",
                                        "Messages",
                                        3,
                                        "/api/v1/portal/aggregation/office-center/messages"
                                )
                        ),
                        dashboard.identity(),
                        dashboard.todo(),
                        dashboard.message(),
                        FIXED_TIME
                );
            }
        };
    }

    private PortalDashboardView dashboardReady() {
        PortalIdentityCard identity = new PortalIdentityCard(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "position-1",
                "organization-1",
                "department-1",
                "Chief Clerk",
                "Head Office",
                "General Office",
                "PRIMARY",
                FIXED_TIME
        );
        return new PortalDashboardView(
                PortalSceneType.HOME,
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.IDENTITY),
                        PortalCardType.IDENTITY,
                        identity,
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.TODO),
                        PortalCardType.TODO,
                        new PortalTodoCard(
                                1,
                                1,
                                Map.of("approval", 1L),
                                java.util.List.of(new PortalTodoItem(
                                        "todo-1",
                                        "Approve budget",
                                        "approval",
                                        "HIGH",
                                        FIXED_TIME.plusSeconds(3600),
                                        FIXED_TIME.minusSeconds(600)
                                ))
                        ),
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.MESSAGE),
                        PortalCardType.MESSAGE,
                        new PortalMessageCard(
                                1,
                                Map.of("TODO_CREATED", 1L),
                                java.util.List.of(new PortalMessageItem(
                                        "notification-1",
                                        "Approve budget",
                                        "TODO_CREATED",
                                        "HIGH",
                                        "/portal/todo/todo-1",
                                        FIXED_TIME.minusSeconds(300)
                                ))
                        ),
                        FIXED_TIME
                ),
                null
        );
    }

    private PortalDashboardView dashboardWithDegradedMessage() {
        PortalDashboardView readyDashboard = dashboardReady();
        return new PortalDashboardView(
                readyDashboard.sceneType(),
                readyDashboard.identity(),
                readyDashboard.todo(),
                PortalCardSnapshot.failed(
                        readyDashboard.message().snapshotKey(),
                        PortalCardType.MESSAGE,
                        PortalMessageCard.empty(),
                        "Message card is temporarily unavailable",
                        FIXED_TIME
                ),
                readyDashboard.content()
        );
    }

    private PortalDashboardView officeCenterDashboardReady() {
        PortalDashboardView homeDashboard = dashboardReady();
        return new PortalDashboardView(
                PortalSceneType.OFFICE_CENTER,
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(
                                homeDashboard.identity().data(),
                                PortalSceneType.OFFICE_CENTER,
                                PortalCardType.IDENTITY
                        ),
                        PortalCardType.IDENTITY,
                        homeDashboard.identity().data(),
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(
                                homeDashboard.identity().data(),
                                PortalSceneType.OFFICE_CENTER,
                                PortalCardType.TODO
                        ),
                        PortalCardType.TODO,
                        homeDashboard.todo().data(),
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(
                                homeDashboard.identity().data(),
                                PortalSceneType.OFFICE_CENTER,
                                PortalCardType.MESSAGE
                        ),
                        PortalCardType.MESSAGE,
                        homeDashboard.message().data(),
                        FIXED_TIME
                ),
                null
        );
    }

    private PortalTemplateApplicationService templateApplicationService(InMemoryPortalTemplateRepository repository) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PortalPublicationApplicationService publicationApplicationService() {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private void overrideTemplatePages(
            InMemoryPortalTemplateRepository repository,
            String templateId,
            List<PortalPage> pages
    ) {
        PortalTemplate existing = repository.findByTemplateId(templateId).orElseThrow();
        repository.save(new PortalTemplate(
                existing.templateId(),
                existing.tenantId(),
                existing.templateCode(),
                existing.displayName(),
                existing.sceneType(),
                pages,
                existing.versions(),
                existing.publishedSnapshots(),
                existing.createdAt(),
                FIXED_TIME.plusSeconds(60)
        ));
    }

    private List<PortalPage> publishedPages() {
        return List.of(new PortalPage(
                "page-home-published",
                "home-published",
                "Home Published",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new PortalLayoutRegion(
                        "region-work-focus",
                        "work-focus",
                        "Work Focus",
                        true,
                        List.of(
                                placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                        )
                ))
        ));
    }

    private List<PortalPage> overlayPages() {
        return List.of(new PortalPage(
                "page-home-overlay",
                "home-overlay",
                "Home Overlay",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new PortalLayoutRegion(
                        "region-work-focus",
                        "work-focus",
                        "Work Focus",
                        true,
                        List.of(
                                placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                placement("placement-todo", "todo-card", WidgetCardType.TODO, 20),
                                placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30)
                        )
                ))
        ));
    }

    private List<PortalPage> draftPages() {
        return List.of(new PortalPage(
                "page-home-draft",
                "home-draft",
                "Home Draft",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new PortalLayoutRegion(
                        "region-identity-overview",
                        "identity-overview",
                        "Identity Overview",
                        true,
                        List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 10))
                ))
        ));
    }

    private PortalWidgetPlacement placement(
            String placementCode,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new PortalWidgetPlacement(
                placementCode,
                placementCode,
                widgetCode,
                cardType,
                orderNo,
                false,
                false,
                Map.of()
        );
    }

    private PersonalizationIdentityContext identityContext(String assignmentId, String positionId, String personId) {
        return new PersonalizationIdentityContext("tenant-1", personId, assignmentId, positionId);
    }

    private PortalHomeRefreshStateApplicationService refreshStateApplicationService() {
        return new PortalHomeRefreshStateApplicationService(
                new InMemoryPortalHomeRefreshStateRepository(),
                () -> identityContext("assignment-1", "position-1", "person-1"),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PortalHomePersonalizationOverlayProvider noOverlayProvider() {
        return sceneType -> PortalHomePersonalizationOverlay.none();
    }

    private PortalHomePersonalizationOverlayProvider overlayProvider(
            String basePublicationId,
            List<String> widgetOrderOverride,
            List<String> hiddenPlacementCodes
    ) {
        return sceneType -> new PortalHomePersonalizationOverlay(
                basePublicationId,
                widgetOrderOverride,
                hiddenPlacementCodes
        );
    }
}
