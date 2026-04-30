package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.data.governance.domain.AlertRule;
import com.hjo2oa.data.governance.domain.GovernanceProfile;
import com.hjo2oa.data.governance.domain.GovernanceProfileRepository;
import com.hjo2oa.data.governance.domain.HealthCheckRule;
import com.hjo2oa.data.governance.domain.ServiceVersionRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
class MybatisGovernanceProfileRepository implements GovernanceProfileRepository {

    private final GovernanceProfileMapper governanceProfileMapper;
    private final HealthCheckRuleMapper healthCheckRuleMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final ServiceVersionRecordMapper serviceVersionRecordMapper;

    MybatisGovernanceProfileRepository(
            GovernanceProfileMapper governanceProfileMapper,
            HealthCheckRuleMapper healthCheckRuleMapper,
            AlertRuleMapper alertRuleMapper,
            ServiceVersionRecordMapper serviceVersionRecordMapper
    ) {
        this.governanceProfileMapper = governanceProfileMapper;
        this.healthCheckRuleMapper = healthCheckRuleMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.serviceVersionRecordMapper = serviceVersionRecordMapper;
    }

    @Override
    public Optional<GovernanceProfile> findByCode(String tenantId, String code) {
        LambdaQueryWrapper<GovernanceProfileEntity> wrapper = new LambdaQueryWrapper<GovernanceProfileEntity>()
                .eq(GovernanceProfileEntity::getTenantId, tenantId)
                .eq(GovernanceProfileEntity::getCode, code);
        return Optional.ofNullable(governanceProfileMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Optional<GovernanceProfile> findByTarget(String tenantId, GovernanceScopeType scopeType, String targetCode) {
        LambdaQueryWrapper<GovernanceProfileEntity> wrapper = new LambdaQueryWrapper<GovernanceProfileEntity>()
                .eq(GovernanceProfileEntity::getTenantId, tenantId)
                .eq(GovernanceProfileEntity::getScopeType, scopeType.name())
                .eq(GovernanceProfileEntity::getTargetCode, targetCode);
        return Optional.ofNullable(governanceProfileMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<GovernanceProfile> findByTenant(String tenantId) {
        return governanceProfileMapper.selectList(new LambdaQueryWrapper<GovernanceProfileEntity>()
                        .eq(GovernanceProfileEntity::getTenantId, tenantId))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(GovernanceProfile::code))
                .toList();
    }

    @Override
    public List<GovernanceProfile> findAllActive() {
        return governanceProfileMapper.selectAllActiveForRuntime(GovernanceProfileStatus.ACTIVE.name())
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(GovernanceProfile::code))
                .toList();
    }

    @Override
    public GovernanceProfile save(GovernanceProfile profile) {
        GovernanceProfileEntity entity = toEntity(profile);
        if (governanceProfileMapper.selectById(profile.governanceId()) == null) {
            governanceProfileMapper.insert(entity);
        } else {
            governanceProfileMapper.updateById(entity);
        }

        healthCheckRuleMapper.deleteByGovernanceIdPhysically(profile.governanceId());
        for (HealthCheckRule rule : profile.healthCheckRules()) {
            healthCheckRuleMapper.insert(toEntity(rule));
        }

        alertRuleMapper.deleteByGovernanceIdPhysically(profile.governanceId());
        for (AlertRule rule : profile.alertRules()) {
            alertRuleMapper.insert(toEntity(rule));
        }

        serviceVersionRecordMapper.deleteByGovernanceIdPhysically(profile.governanceId());
        for (ServiceVersionRecord record : profile.serviceVersionRecords()) {
            serviceVersionRecordMapper.insert(toEntity(record, profile.createdAt(), profile.updatedAt()));
        }
        return profile;
    }

    private GovernanceProfile toDomain(GovernanceProfileEntity entity) {
        List<HealthCheckRule> healthRules = healthCheckRuleMapper.selectList(new LambdaQueryWrapper<HealthCheckRuleEntity>()
                        .eq(HealthCheckRuleEntity::getGovernanceId, entity.getGovernanceId()))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(HealthCheckRule::ruleCode))
                .toList();
        List<AlertRule> alertRules = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRuleEntity>()
                        .eq(AlertRuleEntity::getGovernanceId, entity.getGovernanceId()))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(AlertRule::ruleCode))
                .toList();
        List<ServiceVersionRecord> versionRecords = serviceVersionRecordMapper.selectList(
                        new LambdaQueryWrapper<ServiceVersionRecordEntity>()
                                .eq(ServiceVersionRecordEntity::getGovernanceId, entity.getGovernanceId()))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(ServiceVersionRecord::registeredAt).reversed())
                .toList();
        return new GovernanceProfile(
                entity.getGovernanceId(),
                entity.getCode(),
                GovernanceScopeType.valueOf(entity.getScopeType()),
                entity.getTargetCode(),
                entity.getSlaPolicyJson(),
                entity.getAlertPolicyJson(),
                GovernanceProfileStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                healthRules,
                alertRules,
                versionRecords
        );
    }

    private HealthCheckRule toDomain(HealthCheckRuleEntity entity) {
        return new HealthCheckRule(
                entity.getRuleId(),
                entity.getGovernanceId(),
                entity.getRuleCode(),
                entity.getRuleName(),
                HealthCheckType.valueOf(entity.getCheckType()),
                HealthCheckSeverity.valueOf(entity.getSeverity()),
                HealthCheckRuleStatus.valueOf(entity.getStatus()),
                entity.getMetricName(),
                ComparisonOperator.valueOf(entity.getComparisonOperator()),
                entity.getThresholdValue(),
                entity.getWindowMinutes(),
                entity.getDedupMinutes(),
                entity.getScheduleExpression(),
                entity.getStrategyJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AlertRule toDomain(AlertRuleEntity entity) {
        return new AlertRule(
                entity.getRuleId(),
                entity.getGovernanceId(),
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.getSourceRuleCode(),
                entity.getMetricName(),
                entity.getAlertType(),
                AlertLevel.valueOf(entity.getAlertLevel()),
                AlertRuleStatus.valueOf(entity.getStatus()),
                ComparisonOperator.valueOf(entity.getComparisonOperator()),
                entity.getThresholdValue(),
                entity.getDedupMinutes(),
                entity.getEscalationMinutes(),
                entity.getNotificationPolicyJson(),
                entity.getStrategyJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ServiceVersionRecord toDomain(ServiceVersionRecordEntity entity) {
        return new ServiceVersionRecord(
                entity.getVersionRecordId(),
                entity.getGovernanceId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                entity.getVersion(),
                entity.getCompatibilityNote(),
                entity.getChangeSummary(),
                ServiceVersionStatus.valueOf(entity.getStatus()),
                entity.getRegisteredAt(),
                entity.getPublishedAt(),
                entity.getDeprecatedAt(),
                entity.getOperatorId(),
                entity.getApprovalNote(),
                entity.getAuditTraceId()
        );
    }

    private GovernanceProfileEntity toEntity(GovernanceProfile profile) {
        return new GovernanceProfileEntity(
                profile.governanceId(),
                profile.code(),
                profile.scopeType().name(),
                profile.targetCode(),
                profile.slaPolicyJson(),
                profile.alertPolicyJson(),
                profile.status().name(),
                profile.tenantId(),
                0L,
                0,
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    private HealthCheckRuleEntity toEntity(HealthCheckRule rule) {
        return new HealthCheckRuleEntity(
                rule.ruleId(),
                rule.governanceId(),
                rule.ruleCode(),
                rule.ruleName(),
                rule.checkType().name(),
                rule.severity().name(),
                rule.status().name(),
                rule.metricName(),
                rule.comparisonOperator().name(),
                rule.thresholdValue(),
                rule.windowMinutes(),
                rule.dedupMinutes(),
                rule.scheduleExpression(),
                rule.strategyJson(),
                0L,
                0,
                rule.createdAt(),
                rule.updatedAt()
        );
    }

    private AlertRuleEntity toEntity(AlertRule rule) {
        return new AlertRuleEntity(
                rule.ruleId(),
                rule.governanceId(),
                rule.ruleCode(),
                rule.ruleName(),
                rule.sourceRuleCode(),
                rule.metricName(),
                rule.alertType(),
                rule.alertLevel().name(),
                rule.status().name(),
                rule.comparisonOperator().name(),
                rule.thresholdValue(),
                rule.dedupMinutes(),
                rule.escalationMinutes(),
                rule.notificationPolicyJson(),
                rule.strategyJson(),
                0L,
                0,
                rule.createdAt(),
                rule.updatedAt()
        );
    }

    private ServiceVersionRecordEntity toEntity(ServiceVersionRecord record, Instant createdAt, Instant updatedAt) {
        return new ServiceVersionRecordEntity(
                record.versionRecordId(),
                record.governanceId(),
                record.targetType().name(),
                record.targetCode(),
                record.version(),
                record.compatibilityNote(),
                record.changeSummary(),
                record.status().name(),
                record.registeredAt(),
                record.publishedAt(),
                record.deprecatedAt(),
                record.operatorId(),
                record.approvalNote(),
                record.auditTraceId(),
                0L,
                0,
                createdAt,
                updatedAt
        );
    }
}
