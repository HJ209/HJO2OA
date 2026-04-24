package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePreviewApplicationService;
import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.infrastructure.InMemoryPersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
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
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplatePreviewControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T21:15:00Z");

    @Test
    void shouldReturnDesignerTemplatePreview() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
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
                ))
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-live",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-42")
        ));
        fixture.personalizationRepository().save(
                PersonalizationProfile.create(
                                "profile-1",
                                "tenant-42",
                                "person-42",
                                "assignment-42",
                                PersonalizationSceneType.HOME,
                                "publication-live",
                                FIXED_TIME
                        )
                        .saveOverrides(
                                null,
                                List.of("identity-card", "todo-card"),
                                List.of("placement-message"),
                                List.of(),
                                FIXED_TIME.plusSeconds(60)
                        )
        );
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/preview")
                        .param("clientType", "PC")
                        .param("tenantId", "tenant-42")
                        .param("personId", "person-42")
                        .param("accountId", "account-42")
                        .param("assignmentId", "assignment-42")
                        .param("positionId", "position-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.clientType").value("PC"))
                .andExpect(jsonPath("$.data.previewIdentity.personId").value("person-42"))
                .andExpect(jsonPath("$.data.previewIdentity.assignmentId").value("assignment-42"))
                .andExpect(jsonPath("$.data.previewIdentity.positionId").value("position-42"))
                .andExpect(jsonPath("$.data.overlay.status").value("applied"))
                .andExpect(jsonPath("$.data.overlay.baselinePublicationId").value("publication-live"))
                .andExpect(jsonPath("$.data.overlay.resolvedLivePublicationId").value("publication-live"))
                .andExpect(jsonPath("$.data.overlay.reason").value("publication-matched"))
                .andExpect(jsonPath("$.data.page.sceneType").value("HOME"))
                .andExpect(jsonPath("$.data.page.regions[0].cards[0].cardType").value("IDENTITY"))
                .andExpect(jsonPath("$.data.page.regions[0].cards[1].cardType").value("TODO"));
    }

    @Test
    void shouldReturnNotFoundWhenPreviewTemplateIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(fixture().applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/missing-template/preview")
                        .param("clientType", "PC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectRepairRequiredWidgetReferenceWhenPreviewingDesignerTemplate() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10))
                        ))
                ))
        ));
        fixture.widgetReferenceStatusApplicationService().markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-message",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/preview")
                        .param("clientType", "PC"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("widgetCode=message-card")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "placementCode=placement-message"
                )));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplatePreviewApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplatePreviewController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        InMemoryPersonalizationProfileRepository personalizationRepository = new InMemoryPersonalizationProfileRepository();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        PortalTemplateCanvasApplicationService canvasApplicationService = new PortalTemplateCanvasApplicationService(
                templateRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                publicationRepository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        MutablePersonalizationBasePublicationResolver basePublicationResolver =
                new MutablePersonalizationBasePublicationResolver(sceneType -> Optional.empty());
        PersonalizationProfileApplicationService personalizationProfileApplicationService =
                new PersonalizationProfileApplicationService(
                        personalizationRepository,
                        () -> new PersonalizationIdentityContext(
                                "tenant-1",
                                "person-1",
                                "assignment-1",
                                "position-1"
                        ),
                        basePublicationResolver,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        return new TestFixture(
                templateApplicationService,
                canvasApplicationService,
                widgetReferenceStatusApplicationService,
                publicationApplicationService,
                personalizationRepository,
                new PortalDesignerTemplatePreviewApplicationService(
                        templateApplicationService,
                        canvasApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                        widgetReferenceStatusApplicationService,
                        activeTemplateResolutionApplicationService,
                        personalizationProfileApplicationService
                )
        );
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

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService canvasApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            InMemoryPersonalizationProfileRepository personalizationRepository,
            PortalDesignerTemplatePreviewApplicationService applicationService
    ) {
    }
}
