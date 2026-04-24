package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublishApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplatePublishControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T18:30:00Z");

    @Test
    void shouldPublishDesignerTemplate() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNo":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.publishedVersionNo").value(1))
                .andExpect(jsonPath("$.data.versions[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldRejectInvalidDesignerTemplateVersion() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNo":3
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectRepairRequiredWidgetReferenceWhenPublishingDesignerTemplate() throws Exception {
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
                        "page-home-custom",
                        "home-custom",
                        "Home Custom",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(new PortalWidgetPlacement(
                                        "placement-message",
                                        "placement-message",
                                        "message-card",
                                        WidgetCardType.MESSAGE,
                                        10,
                                        false,
                                        false,
                                        Map.of()
                                ))
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

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNo":1
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("widgetCode=message-card")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "placementCode=placement-message"
                )));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplatePublishApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplatePublishController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        PortalDesignerTemplateEventListener eventListener =
                new PortalDesignerTemplateEventListener(projectionRepository);
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());

        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                contextProvider,
                event -> {
                    if (event instanceof PortalTemplateCreatedEvent createdEvent) {
                        eventListener.onTemplateCreated(createdEvent);
                    } else if (event instanceof PortalTemplatePublishedEvent publishedEvent) {
                        eventListener.onTemplatePublished(publishedEvent);
                    } else if (event instanceof PortalTemplateDeprecatedEvent deprecatedEvent) {
                        eventListener.onTemplateDeprecated(deprecatedEvent);
                    }
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );

        return new TestFixture(
                templateApplicationService,
                new PortalTemplateCanvasApplicationService(
                        templateRepository,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                        widgetReferenceStatusApplicationService
                ),
                widgetReferenceStatusApplicationService,
                new PortalDesignerTemplatePublishApplicationService(
                        templateApplicationService,
                        new PortalDesignerTemplateStatusApplicationService(projectionRepository)
                )
        );
    }

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService canvasApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalDesignerTemplatePublishApplicationService applicationService
    ) {
    }
}
