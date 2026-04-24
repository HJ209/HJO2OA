package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateInitializationApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplateInitializationControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T16:30:00Z");

    @Test
    void shouldReturnDesignerTemplateInitialization() throws Exception {
        TestFixture fixture = fixture();
        fixture.projectionRepository().save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.widgetDefinitionApplicationService().upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "home-message",
                "Home Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                5
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateCode").value("home-default"))
                .andExpect(jsonPath("$.data.status.templateCode").value("home-default"))
                .andExpect(jsonPath("$.data.canvas.pages[0].pageCode").value("home-main"))
                .andExpect(jsonPath("$.data.widgetPalette.widgets[0].widgetCode").value("home-message"));
    }

    @Test
    void shouldReturnNotFoundWhenDesignerTemplateInitializationIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(fixture().applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/init"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplateInitializationApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplateInitializationController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();

        PortalModelContextProvider portalModelContextProvider =
                () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                portalModelContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        WidgetConfigContextProvider widgetContextProvider =
                () -> new WidgetConfigContext("tenant-1", "portal-admin");
        WidgetDefinitionApplicationService widgetDefinitionApplicationService =
                new WidgetDefinitionApplicationService(
                        new InMemoryWidgetDefinitionRepository(),
                        widgetContextProvider,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        PortalDesignerTemplateInitializationApplicationService applicationService =
                new PortalDesignerTemplateInitializationApplicationService(
                        new PortalDesignerTemplateStatusApplicationService(projectionRepository),
                        new PortalDesignerTemplateWidgetPaletteApplicationService(
                                projectionRepository,
                                widgetDefinitionApplicationService
                        ),
                        new PortalTemplateCanvasApplicationService(templateRepository)
                );

        return new TestFixture(
                projectionRepository,
                templateApplicationService,
                widgetDefinitionApplicationService,
                applicationService
        );
    }

    private record TestFixture(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            PortalTemplateApplicationService templateApplicationService,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            PortalDesignerTemplateInitializationApplicationService applicationService
    ) {
    }
}
