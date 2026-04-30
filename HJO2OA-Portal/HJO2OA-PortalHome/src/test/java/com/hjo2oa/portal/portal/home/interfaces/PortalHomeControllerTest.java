package com.hjo2oa.portal.portal.home.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlay;
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlayProvider;
import com.hjo2oa.portal.portal.home.application.PortalHomePageAssemblyApplicationService;
import com.hjo2oa.portal.portal.home.application.PortalHomeRefreshStateApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
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
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalHomeControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T12:00:00Z");

    @Test
    void shouldReturnPageUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-home-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.layoutType").value("THREE_SECTION"))
                .andExpect(jsonPath("$.data.refreshState.status").value("IDLE"))
                .andExpect(jsonPath("$.data.regions[1].cards[0].cardType").value("TODO"))
                .andExpect(jsonPath("$.data.regions[1].cards[1].cardType").value("MESSAGE"))
                .andExpect(jsonPath("$.meta.requestId").value("req-home-1"));
    }

    @Test
    void shouldRejectInvalidSceneTypeAsBadRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnRefreshStateUsingSharedWebContract() throws Exception {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        refreshStateApplicationService.markCardFailed(
                PortalHomeSceneType.HOME,
                PortalCardType.MESSAGE,
                "portal.snapshot.failed",
                "Message card is temporarily unavailable",
                FIXED_TIME.plusSeconds(30)
        );
        MockMvc mockMvc = buildMockMvc(refreshStateApplicationService, defaultProvider());

        mockMvc.perform(get("/api/v1/portal/home/refresh-state")
                        .param("sceneType", "HOME")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-home-refresh-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("CARD_FAILED"))
                .andExpect(jsonPath("$.data.cardType").value("MESSAGE"))
                .andExpect(jsonPath("$.meta.requestId").value("req-home-refresh-1"));
    }

    @Test
    void shouldReturnOfficeCenterPageWithDynamicNavigationBadgeCounts() throws Exception {
        MockMvc mockMvc = buildMockMvc(
                refreshStateApplicationService(),
                officeCenterProvider()
        );

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "OFFICE_CENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.layoutType").value("OFFICE_SPLIT"))
                .andExpect(jsonPath("$.data.navigation[0].code").value("pending"))
                .andExpect(jsonPath("$.data.navigation[0].badgeCount").value(2))
                .andExpect(jsonPath("$.data.navigation[3].code").value("messages"))
                .andExpect(jsonPath("$.data.navigation[3].badgeCount").value(3));
    }

    @Test
    void shouldReturnMobileWorkbenchPageWithDynamicNavigationBadgeCounts() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "MOBILE_WORKBENCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.layoutType").value("MOBILE_LIGHT"))
                .andExpect(jsonPath("$.data.navigation[0].code").value("tasks"))
                .andExpect(jsonPath("$.data.navigation[0].badgeCount").value(1))
                .andExpect(jsonPath("$.data.navigation[1].code").value("messages"))
                .andExpect(jsonPath("$.data.navigation[1].badgeCount").value(1));
    }

    @Test
    void shouldReturnSourceTemplateMetadataWhenAvailable() throws Exception {
        StaticPortalHomePageTemplateProvider staticProvider = new StaticPortalHomePageTemplateProvider();
        MockMvc mockMvc = buildMockMvc(
                refreshStateApplicationService(),
                defaultProvider(),
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
                )
        );

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sourceTemplateMetadata.templateCode").value("home-default"))
                .andExpect(jsonPath("$.data.sourceTemplateMetadata.publicationId").value("publication-1"))
                .andExpect(jsonPath("$.data.sourceTemplateMetadata.clientType").value("ALL"));
    }

    @Test
    void shouldReturnDifferentSourceTemplateMetadataForDifferentIdentities() throws Exception {
        AtomicReference<PersonalizationIdentityContext> identity = new AtomicReference<>(new PersonalizationIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        ));
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-default",
                "home-default",
                "Home Default Template",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-default", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-home-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-assignment",
                "home-assignment",
                "Home Assignment Template",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-assignment", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-home-assignment",
                "template-assignment",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));
        MockMvc mockMvc = buildMockMvc(
                refreshStateApplicationService(),
                defaultProvider(),
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        identity::get
                )
        );

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceTemplateMetadata.publicationId").value("publication-home-assignment"))
                .andExpect(jsonPath("$.data.branding.title").value("Home Assignment Template"));

        identity.set(new PersonalizationIdentityContext(
                "tenant-1",
                "person-2",
                "assignment-2",
                "position-2"
        ));

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceTemplateMetadata.publicationId").value("publication-home-default"))
                .andExpect(jsonPath("$.data.branding.title").value("Home Default Template"));
    }

    @Test
    void shouldReturnPageWithAppliedLiveOverlayWhenPublicationMatchesProfile() throws Exception {
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
        MockMvc mockMvc = buildMockMvc(
                refreshStateApplicationService(),
                defaultProvider(),
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> new PersonalizationIdentityContext("tenant-1", "person-1", "assignment-1", "position-1")
                ),
                overlayProvider(
                        "publication-1",
                        List.of("todo-card", "identity-card"),
                        List.of("placement-message")
                )
        );

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.regions[0].cards.length()").value(2))
                .andExpect(jsonPath("$.data.regions[0].cards[0].cardType").value("TODO"))
                .andExpect(jsonPath("$.data.regions[0].cards[1].cardType").value("IDENTITY"));
    }

    @Test
    void shouldReturnDefaultTemplateStructureWhenOverlayPublicationDoesNotMatch() throws Exception {
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
        MockMvc mockMvc = buildMockMvc(
                refreshStateApplicationService(),
                defaultProvider(),
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        () -> new PersonalizationIdentityContext("tenant-1", "person-1", "assignment-1", "position-1")
                ),
                overlayProvider(
                        "publication-outdated",
                        List.of("todo-card", "identity-card"),
                        List.of("placement-message")
                )
        );

        mockMvc.perform(get("/api/v1/portal/home/page")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.regions[0].cards.length()").value(3))
                .andExpect(jsonPath("$.data.regions[0].cards[0].cardType").value("MESSAGE"))
                .andExpect(jsonPath("$.data.regions[0].cards[1].cardType").value("TODO"))
                .andExpect(jsonPath("$.data.regions[0].cards[2].cardType").value("IDENTITY"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(
                refreshStateApplicationService(),
                defaultProvider(),
                new StaticPortalHomePageTemplateProvider(),
                noOverlayProvider()
        );
    }

    private MockMvc buildMockMvc(
            PortalHomeRefreshStateApplicationService refreshStateApplicationService,
            PortalHomeAggregationViewProvider aggregationViewProvider
    ) {
        return buildMockMvc(
                refreshStateApplicationService,
                aggregationViewProvider,
                new StaticPortalHomePageTemplateProvider(),
                noOverlayProvider()
        );
    }

    private MockMvc buildMockMvc(
            PortalHomeRefreshStateApplicationService refreshStateApplicationService,
            PortalHomeAggregationViewProvider aggregationViewProvider,
            PortalHomePageTemplateProvider templateProvider
    ) {
        return buildMockMvc(
                refreshStateApplicationService,
                aggregationViewProvider,
                templateProvider,
                noOverlayProvider()
        );
    }

    private MockMvc buildMockMvc(
            PortalHomeRefreshStateApplicationService refreshStateApplicationService,
            PortalHomeAggregationViewProvider aggregationViewProvider,
            PortalHomePageTemplateProvider templateProvider,
            PortalHomePersonalizationOverlayProvider overlayProvider
    ) {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                templateProvider,
                aggregationViewProvider,
                refreshStateApplicationService,
                overlayProvider
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();

        return MockMvcBuilders.standaloneSetup(
                        new PortalHomeController(service, refreshStateApplicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalHomeAggregationViewProvider defaultProvider() {
        return (sceneType, cardTypes) -> dashboardReady();
    }

    private PortalHomeAggregationViewProvider officeCenterProvider() {
        return new PortalHomeAggregationViewProvider() {
            @Override
            public PortalDashboardView dashboard(PortalHomeSceneType sceneType, java.util.Set<PortalCardType> cardTypes) {
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

    private PortalTemplateApplicationService templateApplicationService(InMemoryPortalTemplateRepository repository) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, java.time.ZoneOffset.UTC)
        );
    }

    private PortalPublicationApplicationService publicationApplicationService() {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, java.time.ZoneOffset.UTC)
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

    private PortalHomeRefreshStateApplicationService refreshStateApplicationService() {
        return new PortalHomeRefreshStateApplicationService(
                new InMemoryPortalHomeRefreshStateRepository(),
                () -> new PersonalizationIdentityContext("tenant-1", "person-1", "assignment-1", "position-1")
        );
    }
}
