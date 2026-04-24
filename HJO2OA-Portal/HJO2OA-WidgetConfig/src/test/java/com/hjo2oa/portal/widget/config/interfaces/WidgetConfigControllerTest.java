package com.hjo2oa.portal.widget.config.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WidgetConfigControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T05:00:00Z");

    @Test
    void shouldUpsertWidgetUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(put("/api/v1/portal/widget-config/widgets/widget-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-widget-upsert-1")
                        .content("""
                                {
                                  "widgetCode":"todo-card",
                                  "displayName":"Todo Card",
                                  "cardType":"TODO",
                                  "sceneType":"OFFICE_CENTER",
                                  "sourceModule":"todo-center",
                                  "dataSourceType":"AGGREGATION_QUERY",
                                  "allowHide":true,
                                  "allowCollapse":true,
                                  "maxItems":8
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.widgetCode").value("todo-card"))
                .andExpect(jsonPath("$.data.cardType").value("TODO"))
                .andExpect(jsonPath("$.meta.requestId").value("req-widget-upsert-1"));
    }

    @Test
    void shouldReturnCurrentWidgetDefinition() throws Exception {
        WidgetDefinitionApplicationService applicationService = applicationService();
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "message-card",
                "Message Card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                5
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/widget-config/widgets/widget-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.displayName").value("Message Card"));
    }

    @Test
    void shouldReturnWidgetDefinitionListWithOptionalFilters() throws Exception {
        WidgetDefinitionApplicationService applicationService = applicationService();
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
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
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
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
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-3",
                "office-message",
                "Office Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.OFFICE_CENTER,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                6
        ));
        applicationService.disable(new com.hjo2oa.portal.widget.config.application.DisableWidgetDefinitionCommand("widget-3"));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/widget-config/widgets")
                        .param("sceneType", "OFFICE_CENTER")
                        .param("status", WidgetDefinitionStatus.ACTIVE.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].widgetId").value("widget-2"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void shouldDisableWidgetUsingSharedWebContract() throws Exception {
        WidgetDefinitionApplicationService applicationService = applicationService();
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "identity-card",
                "Identity Card",
                WidgetCardType.IDENTITY,
                WidgetSceneType.HOME,
                "identity-context",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                false,
                1
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/portal/widget-config/widgets/widget-1/disable")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-widget-disable-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.meta.requestId").value("req-widget-disable-1"));
    }

    @Test
    void shouldRejectDuplicateWidgetCodeAsConflict() throws Exception {
        WidgetDefinitionApplicationService applicationService = applicationService();
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "todo-card",
                "Todo Card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/portal/widget-config/widgets/widget-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "widgetCode":"todo-card",
                                  "displayName":"Another Todo Card",
                                  "cardType":"TODO",
                                  "sceneType":"HOME",
                                  "sourceModule":"todo-center",
                                  "dataSourceType":"AGGREGATION_QUERY",
                                  "allowHide":true,
                                  "allowCollapse":true,
                                  "maxItems":8
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(WidgetDefinitionApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new WidgetConfigController(applicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private WidgetDefinitionApplicationService applicationService() {
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
