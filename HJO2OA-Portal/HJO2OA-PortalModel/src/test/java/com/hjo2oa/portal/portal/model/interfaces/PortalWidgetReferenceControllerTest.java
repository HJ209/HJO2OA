package com.hjo2oa.portal.portal.model.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
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

class PortalWidgetReferenceControllerTest {

    @Test
    void shouldReturnCurrentWidgetReferenceStatus() throws Exception {
        PortalWidgetReferenceStatusApplicationService applicationService = applicationService();
        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T09:40:00Z"),
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                List.of("displayName")
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/model/widget-references/widget-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.widgetId").value("widget-1"))
                .andExpect(jsonPath("$.data.state").value("STALE"));
    }

    @Test
    void shouldReturnNotFoundWhenWidgetReferenceStatusMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService());

        mockMvc.perform(get("/api/v1/portal/model/widget-references/widget-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(PortalWidgetReferenceStatusApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalWidgetReferenceController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalWidgetReferenceStatusApplicationService applicationService() {
        return new PortalWidgetReferenceStatusApplicationService(
                new InMemoryPortalWidgetReferenceStatusRepository()
        );
    }
}
