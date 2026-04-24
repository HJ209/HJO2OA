package com.hjo2oa.data.governance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.governance.application.GovernanceCommands.AlertActionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.ManualGovernanceInterventionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataGovernanceAlertedEvent;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionResult;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceProfileRepository;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceRuntimeRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GovernanceMonitoringApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:00:00Z");

    @Test
    void shouldCreateAlertTraceAndPublishNotificationWhenSyncFails() {
        Fixture fixture = fixture("{\"allowedActions\":[\"REQUEST_COMPENSATION\",\"REQUEST_RETRY\"]}");

        fixture.monitoringApplicationService().handleDataSyncFailed(new GovernanceContractEvents.DataSyncFailedEvent(
                java.util.UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "sync-task-1",
                "sync.order.export",
                "execution-1",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));

        List<GovernanceAlertRecord> alerts = fixture.runtimeRepository().findAlerts(new AlertQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null,
                null
        ));
        assertThat(alerts).hasSize(1);
        GovernanceAlertRecord alert = alerts.get(0);
        assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(fixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null
        ))).isNotEmpty();
        assertThat(fixture.publishedEvents()).singleElement().isInstanceOf(DataGovernanceAlertedEvent.class);
        DataGovernanceAlertedEvent notifiedEvent = (DataGovernanceAlertedEvent) fixture.publishedEvents().get(0);
        assertThat(notifiedEvent.targetCode()).isEqualTo("sync.order.export");
        assertThat(notifiedEvent.alertLevel()).isEqualTo(AlertLevel.ERROR);
    }

    @Test
    void shouldRequireAcknowledgeBeforeClosingAlert() {
        Fixture fixture = fixture("{\"allowedActions\":[\"REQUEST_COMPENSATION\"]}");
        fixture.monitoringApplicationService().handleDataSyncFailed(new GovernanceContractEvents.DataSyncFailedEvent(
                java.util.UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "sync-task-2",
                "sync.order.export",
                "execution-2",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));
        GovernanceAlertRecord alert = fixture.runtimeRepository().findAlerts(new AlertQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null,
                null
        )).get(0);

        assertThatThrownBy(() -> fixture.monitoringApplicationService().handleAlertAction(new AlertActionCommand(
                "tenant-1",
                alert.alertId(),
                GovernanceActionType.CLOSE_ALERT,
                "ops-1",
                "运维",
                "直接关闭",
                "req-close-1"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("acknowledged or escalated");

        GovernanceAlertRecord acknowledged = fixture.monitoringApplicationService().handleAlertAction(new AlertActionCommand(
                "tenant-1",
                alert.alertId(),
                GovernanceActionType.ACKNOWLEDGE_ALERT,
                "ops-1",
                "运维",
                "已接单",
                "req-ack-1"
        ));
        GovernanceAlertRecord closed = fixture.monitoringApplicationService().handleAlertAction(new AlertActionCommand(
                "tenant-1",
                acknowledged.alertId(),
                GovernanceActionType.CLOSE_ALERT,
                "ops-1",
                "运维",
                "人工完成处理",
                "req-close-2"
        ));

        assertThat(closed.status()).isEqualTo(AlertStatus.CLOSED);
        assertThat(fixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                TraceStatus.RESOLVED,
                null,
                null
        ))).isNotEmpty();
    }

    @Test
    void shouldAllowManualCompensationOnlyWhenProfilePolicyExplicitlyEnablesAction() {
        Fixture deniedFixture = fixture("{\"allowedActions\":[]}");
        deniedFixture.monitoringApplicationService().handleDataSyncFailed(new GovernanceContractEvents.DataSyncFailedEvent(
                java.util.UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "sync-task-3",
                "sync.order.export",
                "execution-3",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));
        String deniedTraceId = deniedFixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null
        )).get(0).traceId();

        assertThatThrownBy(() -> deniedFixture.monitoringApplicationService().submitIntervention(
                new ManualGovernanceInterventionCommand(
                        "tenant-1",
                        GovernanceScopeType.SYNC,
                        "sync.order.export",
                        deniedTraceId,
                        GovernanceActionType.REQUEST_COMPENSATION,
                        "ops-2",
                        "运维",
                        "申请人工补偿",
                        "{\"batch\":\"execution-3\"}",
                        "req-intervene-denied"
                )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Strategy denied");
        assertThat(deniedFixture.runtimeRepository().findAudits(new AuditQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null
        ))).anyMatch(audit -> audit.actionResult() == GovernanceActionResult.REJECTED);

        Fixture allowedFixture = fixture("{\"allowedActions\":[\"REQUEST_COMPENSATION\"]}");
        allowedFixture.monitoringApplicationService().handleDataSyncFailed(new GovernanceContractEvents.DataSyncFailedEvent(
                java.util.UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "sync-task-4",
                "sync.order.export",
                "execution-4",
                "SYNC_TIMEOUT",
                "同步超时",
                true
        ));
        String allowedTraceId = allowedFixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                null,
                null,
                null
        )).get(0).traceId();
        GovernanceActionAuditRecord accepted = allowedFixture.monitoringApplicationService().submitIntervention(
                new ManualGovernanceInterventionCommand(
                        "tenant-1",
                        GovernanceScopeType.SYNC,
                        "sync.order.export",
                        allowedTraceId,
                        GovernanceActionType.REQUEST_COMPENSATION,
                        "ops-3",
                        "运维",
                        "批准人工补偿",
                        "{\"batch\":\"execution-4\"}",
                        "req-intervene-allowed"
                )
        );

        assertThat(accepted.actionResult()).isEqualTo(GovernanceActionResult.ACCEPTED);
        assertThat(allowedFixture.runtimeRepository().findTraces(new TraceQuery(
                GovernanceScopeType.SYNC,
                "sync.order.export",
                TraceStatus.COMPENSATED,
                null,
                null
        ))).isNotEmpty();
    }

    private Fixture fixture(String alertPolicyJson) {
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
                "{\"slaSeconds\":300}",
                alertPolicyJson,
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "管理员",
                "req-profile-sync"
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
                "req-health-sync"
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
                "req-alert-sync"
        ));
        List<DomainEvent> publishedEvents = new ArrayList<>();
        GovernanceMonitoringApplicationService monitoringApplicationService = new GovernanceMonitoringApplicationService(
                profileRepository,
                runtimeRepository,
                publishedEvents::add,
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return new Fixture(profileRepository, runtimeRepository, monitoringApplicationService, publishedEvents);
    }

    private record Fixture(
            InMemoryGovernanceProfileRepository profileRepository,
            InMemoryGovernanceRuntimeRepository runtimeRepository,
            GovernanceMonitoringApplicationService monitoringApplicationService,
            List<DomainEvent> publishedEvents
    ) {
    }
}
