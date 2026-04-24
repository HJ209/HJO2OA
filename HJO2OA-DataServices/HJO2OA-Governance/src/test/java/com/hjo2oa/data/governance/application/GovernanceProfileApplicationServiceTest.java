package com.hjo2oa.data.governance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.data.governance.application.GovernanceCommands.DeprecateServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.PublishServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.RegisterServiceVersionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertAlertRuleCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.UpsertHealthCheckRuleCommand;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceProfileRepository;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceRuntimeRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class GovernanceProfileApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T01:00:00Z");

    @Test
    void shouldManageProfileRulesAndVersionsWithAuditTrail() {
        InMemoryGovernanceProfileRepository profileRepository = new InMemoryGovernanceProfileRepository();
        InMemoryGovernanceRuntimeRepository runtimeRepository = new InMemoryGovernanceRuntimeRepository();
        GovernanceProfileApplicationService applicationService = new GovernanceProfileApplicationService(
                profileRepository,
                runtimeRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        applicationService.upsertProfile(new UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-sync-order",
                GovernanceScopeType.SYNC,
                "sync.order.export",
                "{\"slaSeconds\":300}",
                "{\"allowedActions\":[\"REQUEST_RETRY\",\"REQUEST_COMPENSATION\"]}",
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "管理员",
                "req-profile-1"
        ));

        applicationService.upsertHealthCheckRule(new UpsertHealthCheckRuleCommand(
                "tenant-1",
                "gov-sync-order",
                "hc-sync-failure-count",
                "同步失败次数检查",
                HealthCheckType.FAILURE_RATE,
                HealthCheckSeverity.ERROR,
                HealthCheckRuleStatus.ENABLED,
                "FAILURE_COUNT",
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ZERO,
                5,
                10,
                "0 */5 * * * *",
                "{\"window\":\"5m\"}",
                "admin-1",
                "管理员",
                "req-health-1"
        ));

        applicationService.upsertAlertRule(new UpsertAlertRuleCommand(
                "tenant-1",
                "gov-sync-order",
                "ar-sync-failure",
                "同步失败告警",
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
                "{\"manual\":true}",
                "admin-1",
                "管理员",
                "req-alert-1"
        ));

        applicationService.registerVersion(new RegisterServiceVersionCommand(
                "tenant-1",
                "gov-sync-order",
                GovernanceScopeType.SYNC,
                "sync.order.export",
                "v1",
                "与源系统兼容",
                "首次登记",
                "批准登记",
                "admin-1",
                "管理员",
                "req-version-register-1"
        ));
        applicationService.publishVersion(new PublishServiceVersionCommand(
                "tenant-1",
                "gov-sync-order",
                "v1",
                "批准发布",
                "admin-1",
                "管理员",
                "req-version-publish-1"
        ));
        applicationService.deprecateVersion(new DeprecateServiceVersionCommand(
                "tenant-1",
                "gov-sync-order",
                "v1",
                "已切换到 v2",
                "admin-1",
                "管理员",
                "req-version-deprecate-1"
        ));

        assertThat(profileRepository.findByCode("tenant-1", "gov-sync-order")).isPresent();
        var savedProfile = profileRepository.findByCode("tenant-1", "gov-sync-order").orElseThrow();
        assertThat(savedProfile.healthCheckRules()).singleElement().extracting("ruleCode").isEqualTo("hc-sync-failure-count");
        assertThat(savedProfile.alertRules()).singleElement().extracting("ruleCode").isEqualTo("ar-sync-failure");
        assertThat(savedProfile.serviceVersionRecords()).singleElement()
                .extracting("status")
                .isEqualTo(ServiceVersionStatus.DEPRECATED);
        assertThat(runtimeRepository.findAudits(new AuditQuery(GovernanceScopeType.SYNC, "sync.order.export", null, null)))
                .extracting(audit -> audit.actionType().name())
                .containsExactlyInAnyOrder(
                        GovernanceActionType.UPSERT_PROFILE.name(),
                        GovernanceActionType.UPSERT_HEALTH_RULE.name(),
                        GovernanceActionType.UPSERT_ALERT_RULE.name(),
                        GovernanceActionType.REGISTER_VERSION.name(),
                        GovernanceActionType.PUBLISH_VERSION.name(),
                        GovernanceActionType.DEPRECATE_VERSION.name()
                );
    }

    @Test
    void shouldRejectAlertRuleWhenHealthRuleDependencyIsMissing() {
        GovernanceProfileApplicationService applicationService = new GovernanceProfileApplicationService(
                new InMemoryGovernanceProfileRepository(),
                new InMemoryGovernanceRuntimeRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        applicationService.upsertProfile(new UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-report-main",
                GovernanceScopeType.REPORT,
                "report.main",
                null,
                "{\"allowedActions\":[]}",
                GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "管理员",
                "req-profile-2"
        ));

        assertThatThrownBy(() -> applicationService.upsertAlertRule(new UpsertAlertRuleCommand(
                "tenant-1",
                "gov-report-main",
                "ar-report-missing-source",
                "缺少依赖的告警",
                "missing-health-rule",
                null,
                "REPORT_STALE",
                AlertLevel.WARN,
                AlertRuleStatus.ENABLED,
                ComparisonOperator.GREATER_THAN,
                BigDecimal.ONE,
                5,
                15,
                null,
                null,
                "admin-1",
                "管理员",
                "req-alert-2"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("missing-health-rule");
    }
}
