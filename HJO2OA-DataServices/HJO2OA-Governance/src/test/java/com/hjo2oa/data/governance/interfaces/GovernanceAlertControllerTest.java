package com.hjo2oa.data.governance.interfaces;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.application.GovernanceProfileApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceProfileRepository;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceRuntimeRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GovernanceAlertControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T04:00:00Z");

    @Test
    void shouldAcknowledgeAndCloseAlertThroughController() throws Exception {
        Fixture fixture = fixture();
        String alertId = fixture.runtimeRepository().findAlerts(new AlertQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null,
                null
        )).get(0).alertId();
        MockMvc mockMvc = fixture.mockMvc();

        mockMvc.perform(post("/api/v1/data/governance/alerts/" + alertId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "operatorId":"ops-1",
                                  "operatorName":"运维",
                                  "reason":"收到告警",
                                  "requestId":"req-alert-ack"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        mockMvc.perform(post("/api/v1/data/governance/alerts/" + alertId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "operatorId":"ops-1",
                                  "operatorName":"运维",
                                  "reason":"问题已处理",
                                  "requestId":"req-alert-close"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/data/governance/alerts/" + alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void shouldRejectDirectCloseFromOpenAlert() throws Exception {
        Fixture fixture = fixture();
        String alertId = fixture.runtimeRepository().findAlerts(new AlertQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null,
                null
        )).get(0).alertId();

        fixture.mockMvc().perform(post("/api/v1/data/governance/alerts/" + alertId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "operatorId":"ops-1",
                                  "operatorName":"运维",
                                  "reason":"误关",
                                  "requestId":"req-alert-close-direct"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GOVERNANCE_INVALID_STATUS"))
                .andExpect(jsonPath("$.message").value(containsString("acknowledged or escalated")));
    }

    private Fixture fixture() {
        InMemoryGovernanceProfileRepository profileRepository = new InMemoryGovernanceProfileRepository();
        InMemoryGovernanceRuntimeRepository runtimeRepository = new InMemoryGovernanceRuntimeRepository();
        GovernanceProfileApplicationService profileApplicationService = new GovernanceProfileApplicationService(
                profileRepository,
                runtimeRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        profileApplicationService.upsertProfile(new UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-sync-order",
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                "{\"allowedActions\":[\"REQUEST_COMPENSATION\"]}",
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "管理员",
                "req-profile-alert"
        ));
        profileApplicationService.upsertHealthCheckRule(new UpsertHealthCheckRuleCommand(
                "tenant-1",
                "gov-sync-order",
                "hc-sync-failure-count",
                "失败数检查",
                HealthCheckType.CUSTOM,
                HealthCheckSeverity.ERROR,
                HealthCheckRuleStatus.ENABLED,
                "FAILURE_COUNT",
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ZERO,
                5,
                10,
                "0 */5 * * * *",
                null,
                "admin-1",
                "管理员",
                "req-health-alert"
        ));
        profileApplicationService.upsertAlertRule(new UpsertAlertRuleCommand(
                "tenant-1",
                "gov-sync-order",
                "ar-sync-failure",
                "失败告警",
                "hc-sync-failure-count",
                null,
                "SYNC_FAILURE",
                AlertLevel.ERROR,
                AlertRuleStatus.ENABLED,
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ZERO,
                10,
                30,
                "{\"channels\":[\"EVENT\"]}",
                null,
                "admin-1",
                "管理员",
                "req-alert-rule"
        ));
        GovernanceMonitoringApplicationService monitoringApplicationService = new GovernanceMonitoringApplicationService(
                profileRepository,
                runtimeRepository,
                event -> {
                },
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        monitoringApplicationService.handleDataSyncFailed(new GovernanceContractEvents.DataSyncFailedEvent(
                java.util.UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "sync-task-5",
                "sync.order.export",
                "execution-5",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GovernanceAlertController(monitoringApplicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
        return new Fixture(runtimeRepository, mockMvc);
    }

    private record Fixture(InMemoryGovernanceRuntimeRepository runtimeRepository, MockMvc mockMvc) {
    }
}
