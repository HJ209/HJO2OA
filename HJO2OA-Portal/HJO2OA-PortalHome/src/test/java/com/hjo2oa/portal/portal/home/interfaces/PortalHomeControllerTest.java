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
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.portal.portal.home.application.PortalHomePageAssemblyApplicationService;
import com.hjo2oa.portal.portal.home.infrastructure.StaticPortalHomePageTemplateProvider;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
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

    private MockMvc buildMockMvc() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                (sceneType, cardTypes) -> dashboardReady(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();

        return MockMvcBuilders.standaloneSetup(new PortalHomeController(service, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
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
                )
        );
    }
}
