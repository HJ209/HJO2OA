package com.hjo2oa.data.governance.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.application.GovernanceProfileApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceProfileRepository;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceRuntimeRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GovernanceOperationsControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T05:00:00Z");

    @Test
    void shouldRunHealthChecksAndSubmitManualCompensationViaController() throws Exception {
        Fixture fixture = fixture();

        fixture.mockMvc().perform(post("/api/v1/data/governance/health-checks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "targetType":"SYNC",
                                  "targetCode":"sync.order.export",
                                  "operatorId":"ops-1",
                                  "operatorName":"运维",
                                  "requestId":"req-run-health"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].ruleCode").value("hc-sync-failure-count"));

        String traceId = fixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null
        )).get(0).traceId();

        fixture.mockMvc().perform(post("/api/v1/data/governance/interventions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "targetType":"SYNC",
                                  "targetCode":"sync.order.export",
                                  "traceId":"%s",
                                  "actionType":"REQUEST_COMPENSATION",
                                  "operatorId":"ops-2",
                                  "operatorName":"运维",
                                  "reason":"人工补偿",
                                  "payloadJson":"{\\"batch\\":\\"execution-6\\"}",
                                  "requestId":"req-intervention-1"
                                }
                                """.formatted(traceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionType").value("REQUEST_COMPENSATION"))
                .andExpect(jsonPath("$.data.actionResult").value("ACCEPTED"));

        fixture.mockMvc().perform(get("/api/v1/data/governance/audits")
                        .param("targetType", "SYNC")
                        .param("targetCode", "sync.order.export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].actionType").value("REQUEST_COMPENSATION"));
    }

    @Test
    void shouldExposeDefaultHealthSnapshotWhenProfileHasNoRules() throws Exception {
        Fixture fixture = fixtureWithoutRules();

        fixture.mockMvc().perform(post("/api/v1/data/governance/health-checks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "targetType":"API",
                                  "targetCode":"api.invoice.query",
                                  "operatorId":"ops-1",
                                  "operatorName":"ops",
                                  "requestId":"req-run-default-health"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].ruleCode").value("default-runtime-status"))
                .andExpect(jsonPath("$.data.items[0].healthStatus").value("UNKNOWN"));

        fixture.mockMvc().perform(post("/api/v1/data/governance/health-checks/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "targetType":"API",
                                  "targetCode":"api.invoice.query",
                                  "operatorId":"ops-1",
                                  "operatorName":"ops",
                                  "requestId":"req-run-default-health-again"
                                }
                                """))
                .andExpect(status().isOk());

        fixture.mockMvc().perform(get("/api/v1/data/governance/health-snapshots")
                        .param("targetType", "API")
                        .param("targetCode", "api.invoice.query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].ruleCode").value("default-runtime-status"));
    }

    @Test
    void shouldBridgeExternalOpenApiDomainEventByEventType() {
        InMemoryGovernanceProfileRepository profileRepository = new InMemoryGovernanceProfileRepository();
        InMemoryGovernanceRuntimeRepository runtimeRepository = new InMemoryGovernanceRuntimeRepository();
        GovernanceProfileApplicationService profileApplicationService = new GovernanceProfileApplicationService(
                profileRepository,
                runtimeRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        profileApplicationService.upsertProfile(new UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-api-invoice",
                GovernanceScopeType.API,
                "api.invoice.query",
                "{}",
                "{}",
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "admin",
                "req-profile-api-event"
        ));
        profileApplicationService.upsertHealthCheckRule(new UpsertHealthCheckRuleCommand(
                "tenant-1",
                "gov-api-invoice",
                "hc-api-degraded",
                "API degraded",
                HealthCheckType.CUSTOM,
                HealthCheckSeverity.ERROR,
                HealthCheckRuleStatus.ENABLED,
                "STATUS_DEGRADED",
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ZERO,
                5,
                10,
                "0 */5 * * * *",
                null,
                "admin-1",
                "admin",
                "req-health-api-event"
        ));
        profileApplicationService.upsertAlertRule(new UpsertAlertRuleCommand(
                "tenant-1",
                "gov-api-invoice",
                "ar-api-degraded",
                "API degraded alert",
                "hc-api-degraded",
                null,
                "API_DEGRADED",
                AlertLevel.ERROR,
                AlertRuleStatus.ENABLED,
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ZERO,
                10,
                30,
                "{}",
                null,
                "admin-1",
                "admin",
                "req-alert-api-event"
        ));
        List<DomainEvent> publishedEvents = new ArrayList<>();
        GovernanceMonitoringApplicationService monitoringApplicationService = new GovernanceMonitoringApplicationService(
                profileRepository,
                runtimeRepository,
                publishedEvents::add,
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        GovernanceRuntimeEventListener listener = new GovernanceRuntimeEventListener(monitoringApplicationService);

        listener.onGenericDataDomainEvent(new ExternalOpenApiDeprecatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "api-1",
                "api.invoice.query",
                "v1",
                FIXED_TIME,
                null
        ));

        assertThat(runtimeRepository.findSignal("tenant-1", GovernanceScopeType.API, "api.invoice.query"))
                .hasValueSatisfying(signal -> assertThat(signal.runtimeStatus()).isEqualTo(RuntimeTargetStatus.DEGRADED));
        List<GovernanceAlertRecord> alerts = runtimeRepository.findAlerts(new AlertQuery(
                GovernanceScopeType.API,
                "api.invoice.query",
                null,
                AlertStatus.OPEN,
                null,
                null
        ));
        assertThat(alerts).singleElement()
                .satisfies(alert -> {
                    assertThat(alert.alertType()).isEqualTo("API_DEGRADED");
                    assertThat(alert.summary()).isEqualTo("Governance alert triggered: ar-api-degraded");
                });
        assertThat(publishedEvents).singleElement()
                .isInstanceOf(GovernanceContractEvents.DataGovernanceAlertedEvent.class);
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
                "req-profile-ops"
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
                "req-health-ops"
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
                "req-alert-ops"
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
                "sync-task-6",
                "sync.order.export",
                "execution-6",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GovernanceOperationsController(monitoringApplicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
        return new Fixture(runtimeRepository, mockMvc);
    }

    private Fixture fixtureWithoutRules() {
        InMemoryGovernanceProfileRepository profileRepository = new InMemoryGovernanceProfileRepository();
        InMemoryGovernanceRuntimeRepository runtimeRepository = new InMemoryGovernanceRuntimeRepository();
        GovernanceProfileApplicationService profileApplicationService = new GovernanceProfileApplicationService(
                profileRepository,
                runtimeRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        profileApplicationService.upsertProfile(new UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-api-invoice",
                GovernanceScopeType.API,
                "api.invoice.query",
                null,
                "{}",
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "admin",
                "req-profile-default-health"
        ));
        GovernanceMonitoringApplicationService monitoringApplicationService = new GovernanceMonitoringApplicationService(
                profileRepository,
                runtimeRepository,
                event -> {
                },
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GovernanceOperationsController(monitoringApplicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
        return new Fixture(runtimeRepository, mockMvc);
    }

    private record Fixture(InMemoryGovernanceRuntimeRepository runtimeRepository, MockMvc mockMvc) {
    }

    private record ExternalOpenApiDeprecatedEvent(
            UUID eventId,
            Instant occurredAt,
            String tenantId,
            String apiId,
            String code,
            String version,
            Instant deprecatedAt,
            Instant sunsetAt
    ) implements DomainEvent {

        @Override
        public String eventType() {
            return "data.api.deprecated";
        }
    }
}
