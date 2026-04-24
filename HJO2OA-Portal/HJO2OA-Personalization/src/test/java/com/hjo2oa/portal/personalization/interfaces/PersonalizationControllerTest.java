package com.hjo2oa.portal.personalization.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.application.PersonalizationOverlaySaveValidator;
import com.hjo2oa.portal.personalization.application.PortalModelPersonalizationOverlaySaveValidator;
import com.hjo2oa.portal.personalization.application.SavePersonalizationProfileCommand;
import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationResolver;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.infrastructure.InMemoryPersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver;
import com.hjo2oa.portal.personalization.infrastructure.PortalModelActivePublicationProvider;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PersonalizationControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T03:00:00Z");

    @Test
    void shouldSaveProfileUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/portal/personalization/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-personalization-save-1")
                        .content("""
                                {
                                  "sceneType":"OFFICE_CENTER",
                                  "scope":"ASSIGNMENT",
                                  "assignmentId":"assignment-1",
                                  "themeCode":"office-red",
                                  "widgetOrderOverride":["todo-card","message-card"],
                                  "hiddenPlacementCodes":["announcement-card"],
                                  "quickAccessEntries":[
                                    {
                                      "entryType":"PROCESS",
                                      "targetCode":"leave.apply",
                                      "icon":"calendar",
                                      "sortOrder":10,
                                      "pinned":true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sceneType").value("OFFICE_CENTER"))
                .andExpect(jsonPath("$.data.resolvedScope").value("ASSIGNMENT"))
                .andExpect(jsonPath("$.data.quickAccessEntries[0].entryType").value("PROCESS"))
                .andExpect(jsonPath("$.meta.requestId").value("req-personalization-save-1"));
    }

    @Test
    void shouldReturnGlobalProfileWhenAssignmentScopedProfileIsMissing() throws Exception {
        PersonalizationProfileApplicationService applicationService = applicationService();
        applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.GLOBAL,
                null,
                "global-light",
                java.util.List.of("todo-card"),
                java.util.List.of(),
                java.util.List.of()
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/personalization/profile")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.resolvedScope").value("GLOBAL"))
                .andExpect(jsonPath("$.data.themeCode").value("global-light"));
    }

    @Test
    void shouldResetProfileUsingSharedWebContract() throws Exception {
        PersonalizationProfileApplicationService applicationService = applicationService();
        applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.MOBILE_WORKBENCH,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "mobile-blue",
                java.util.List.of("message-card"),
                java.util.List.of("content-card"),
                java.util.List.of()
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/portal/personalization/profile/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-personalization-reset-1")
                        .content("""
                                {
                                  "sceneType":"MOBILE_WORKBENCH",
                                  "scope":"ASSIGNMENT",
                                  "assignmentId":"assignment-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("RESET"))
                .andExpect(jsonPath("$.data.hiddenPlacementCodes").isEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("req-personalization-reset-1"));
    }

    @Test
    void shouldRejectCrossIdentityAssignmentSaveAsBusinessRuleViolation() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/portal/personalization/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneType":"HOME",
                                  "scope":"ASSIGNMENT",
                                  "assignmentId":"assignment-2"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldReturnOffendingPlacementCodeWhenOverlaySaveValidationFails() throws Exception {
        MockMvc mockMvc = buildMockMvc(validatedApplicationService());

        mockMvc.perform(post("/api/v1/portal/personalization/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneType":"HOME",
                                  "scope":"ASSIGNMENT",
                                  "assignmentId":"assignment-1",
                                  "hiddenPlacementCodes":["placement-ghost"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("placement-ghost")));
    }

    @Test
    void shouldReturnDifferentBasePublicationForDifferentIdentitiesInSameScene() throws Exception {
        AtomicReference<PersonalizationIdentityContext> identity = new AtomicReference<>(new PersonalizationIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        ));
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-home-default",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-home-assignment",
                "template-2",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver(
                new PortalModelActivePublicationProvider(publicationApplicationService, identity::get)
        );
        MockMvc mockMvc = buildMockMvc(applicationService(identity::get, resolver));

        mockMvc.perform(get("/api/v1/portal/personalization/profile")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePublicationId").value("publication-home-assignment"));

        identity.set(new PersonalizationIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-2",
                "position-2"
        ));

        mockMvc.perform(get("/api/v1/portal/personalization/profile")
                        .param("sceneType", "HOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePublicationId").value("publication-home-default"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(PersonalizationProfileApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new PersonalizationController(applicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PersonalizationProfileApplicationService applicationService() {
        return applicationService(
                () -> new PersonalizationIdentityContext(
                        "tenant-1",
                        "person-1",
                        "assignment-1",
                        "position-1"
                ),
                new MutablePersonalizationBasePublicationResolver()
        );
    }

    private PersonalizationProfileApplicationService applicationService(
            PersonalizationIdentityContextProvider identityContextProvider,
            PersonalizationBasePublicationResolver basePublicationResolver
    ) {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        return new PersonalizationProfileApplicationService(
                repository,
                identityContextProvider,
                basePublicationResolver,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PersonalizationProfileApplicationService validatedApplicationService() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        MutablePersonalizationBasePublicationResolver basePublicationResolver =
                new MutablePersonalizationBasePublicationResolver();
        PortalModelContextProvider portalContextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        WidgetConfigContextProvider widgetContextProvider = () -> new WidgetConfigContext("tenant-1", "widget-admin");
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        InMemoryWidgetDefinitionRepository widgetRepository = new InMemoryWidgetDefinitionRepository();
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                publicationRepository,
                portalContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                portalContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalTemplateCanvasApplicationService canvasApplicationService = new PortalTemplateCanvasApplicationService(
                templateRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        WidgetDefinitionApplicationService widgetDefinitionApplicationService = new WidgetDefinitionApplicationService(
                widgetRepository,
                widgetContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        upsertWidgetDefinition(widgetDefinitionApplicationService, "widget-message", "message-card", WidgetCardType.MESSAGE);
        upsertWidgetDefinition(widgetDefinitionApplicationService, "widget-todo", "todo-card", WidgetCardType.TODO);
        upsertWidgetDefinition(widgetDefinitionApplicationService, "widget-identity", "identity-card", WidgetCardType.IDENTITY);
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-home",
                "home-template",
                "home-template",
                PortalPublicationSceneType.HOME
        ));
        canvasApplicationService.save(new SavePortalTemplateCanvasCommand(
                "template-home",
                publishedPages()
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-home", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-home-live",
                "template-home",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        basePublicationResolver.bind(PersonalizationSceneType.HOME, "publication-home-live");
        PersonalizationOverlaySaveValidator overlaySaveValidator = new PortalModelPersonalizationOverlaySaveValidator(
                publicationApplicationService,
                canvasApplicationService,
                widgetDefinitionApplicationService
        );
        return new PersonalizationProfileApplicationService(
                repository,
                () -> new PersonalizationIdentityContext(
                        "tenant-1",
                        "person-1",
                        "assignment-1",
                        "position-1"
                ),
                basePublicationResolver,
                event -> {
                },
                overlaySaveValidator,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private List<PortalPage> publishedPages() {
        return List.of(new PortalPage(
                "page-home-default",
                "home-default",
                "Home Default",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(
                        new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                false,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                                )
                        ),
                        new PortalLayoutRegion(
                                "region-identity",
                                "identity-overview",
                                "Identity Overview",
                                false,
                                List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30))
                        )
                )
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

    private void upsertWidgetDefinition(
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            String widgetId,
            String widgetCode,
            WidgetCardType cardType
    ) {
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                widgetId,
                widgetCode,
                widgetCode,
                cardType,
                WidgetSceneType.HOME,
                "aggregation-api",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
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
}
