package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerWidgetPaletteProjectionRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerWidgetPaletteControllerTest {

    @Test
    void shouldReturnCurrentWidgetPalette() throws Exception {
        PortalDesignerWidgetPaletteApplicationService applicationService = applicationService();
        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:30:00Z"),
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                List.of("widgetCode")
        ));
        applicationService.markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:35:00Z"),
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/designer/widget-palette"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalWidgets").value(2))
                .andExpect(jsonPath("$.data.activeWidgets[0].widgetId").value("widget-1"))
                .andExpect(jsonPath("$.data.disabledWidgets[0].widgetId").value("widget-2"));
    }

    @Test
    void shouldReturnEmptyWidgetPaletteWhenNoEventsWereObserved() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/widget-palette"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.totalWidgets").value(0))
                .andExpect(jsonPath("$.data.activeWidgets").isEmpty())
                .andExpect(jsonPath("$.data.disabledWidgets").isEmpty());
    }

    private MockMvc buildMockMvc(PortalDesignerWidgetPaletteApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerWidgetPaletteController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalDesignerWidgetPaletteApplicationService applicationService() {
        return new PortalDesignerWidgetPaletteApplicationService(
                new InMemoryPortalDesignerWidgetPaletteProjectionRepository()
        );
    }
}
