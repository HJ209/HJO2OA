package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
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

class PortalDesignerTemplateWidgetPaletteControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T15:15:00Z");

    @Test
    void shouldReturnTemplateScopedWidgetPalette() throws Exception {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        projectionRepository.save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        WidgetDefinitionApplicationService widgetDefinitionApplicationService = widgetDefinitionApplicationService();
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
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
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-2",
                "office-todo",
                "Office Todo",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
        MockMvc mockMvc = buildMockMvc(projectionRepository, widgetDefinitionApplicationService);

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/widget-palette"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateCode").value("home-default"))
                .andExpect(jsonPath("$.data.widgets.length()").value(1))
                .andExpect(jsonPath("$.data.widgets[0].widgetCode").value("home-message"))
                .andExpect(jsonPath("$.data.widgets[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturnNotFoundWhenTemplateProjectionIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(
                new InMemoryPortalDesignerTemplateProjectionRepository(),
                widgetDefinitionApplicationService()
        );

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-missing/widget-palette"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        PortalDesignerTemplateWidgetPaletteApplicationService applicationService =
                new PortalDesignerTemplateWidgetPaletteApplicationService(
                        projectionRepository,
                        widgetDefinitionApplicationService
                );
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplateWidgetPaletteController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private WidgetDefinitionApplicationService widgetDefinitionApplicationService() {
        WidgetConfigContextProvider contextProvider = () -> new WidgetConfigContext("tenant-1", "portal-admin");
        return new WidgetDefinitionApplicationService(
                new InMemoryWidgetDefinitionRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
