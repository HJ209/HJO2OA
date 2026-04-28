package com.hjo2oa.data.governance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceHealthSnapshot;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeRepository;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeSignal;
import com.hjo2oa.data.governance.domain.GovernanceTraceRecord;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.HealthSnapshotQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionResult;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceHealthStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceTraceType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
class MybatisGovernanceRuntimeRepository implements GovernanceRuntimeRepository {

    private final GovernanceRuntimeSignalMapper signalMapper;
    private final GovernanceHealthSnapshotMapper snapshotMapper;
    private final GovernanceAlertRecordMapper alertMapper;
    private final GovernanceTraceRecordMapper traceMapper;
    private final GovernanceActionAuditMapper auditMapper;

    MybatisGovernanceRuntimeRepository(
            GovernanceRuntimeSignalMapper signalMapper,
            GovernanceHealthSnapshotMapper snapshotMapper,
            GovernanceAlertRecordMapper alertMapper,
            GovernanceTraceRecordMapper traceMapper,
            GovernanceActionAuditMapper auditMapper
    ) {
        this.signalMapper = signalMapper;
        this.snapshotMapper = snapshotMapper;
        this.alertMapper = alertMapper;
        this.traceMapper = traceMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public Optional<GovernanceRuntimeSignal> findSignal(String tenantId, GovernanceScopeType targetType, String targetCode) {
        return Optional.ofNullable(signalMapper.selectOne(new LambdaQueryWrapper<GovernanceRuntimeSignalEntity>()
                        .eq(GovernanceRuntimeSignalEntity::getTenantId, tenantId)
                        .eq(GovernanceRuntimeSignalEntity::getTargetType, targetType.name())
                        .eq(GovernanceRuntimeSignalEntity::getTargetCode, targetCode)))
                .map(this::toDomain);
    }

    @Override
    public List<GovernanceRuntimeSignal> findAllSignals() {
        return signalMapper.selectList(new QueryWrapper<>()).stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(GovernanceRuntimeSignal::updatedAt).reversed())
                .toList();
    }

    @Override
    public GovernanceRuntimeSignal saveSignal(GovernanceRuntimeSignal signal) {
        GovernanceRuntimeSignalEntity entity = toEntity(signal);
        if (signalMapper.selectById(signal.signalId()) == null) {
            signalMapper.insert(entity);
        } else {
            signalMapper.updateById(entity);
        }
        return signal;
    }

    @Override
    public Optional<GovernanceHealthSnapshot> findLatestSnapshot(String governanceId, String ruleId) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<GovernanceHealthSnapshotEntity>()
                        .eq(GovernanceHealthSnapshotEntity::getGovernanceId, governanceId)
                        .eq(GovernanceHealthSnapshotEntity::getRuleId, ruleId))
                .stream()
                .map(this::toDomain)
                .max(Comparator.comparing(GovernanceHealthSnapshot::checkedAt));
    }

    @Override
    public List<GovernanceHealthSnapshot> findSnapshots(HealthSnapshotQuery query) {
        return snapshotMapper.selectList(new QueryWrapper<>()).stream()
                .map(this::toDomain)
                .filter(snapshot -> query.targetType() == null || snapshot.targetType() == query.targetType())
                .filter(snapshot -> query.targetCode() == null || snapshot.targetCode().equals(query.targetCode()))
                .filter(snapshot -> query.ruleCode() == null || snapshot.ruleCode().equals(query.ruleCode()))
                .filter(snapshot -> query.checkedFrom() == null || !snapshot.checkedAt().isBefore(query.checkedFrom()))
                .filter(snapshot -> query.checkedTo() == null || !snapshot.checkedAt().isAfter(query.checkedTo()))
                .sorted(Comparator.comparing(GovernanceHealthSnapshot::checkedAt).reversed())
                .toList();
    }

    @Override
    public GovernanceHealthSnapshot saveSnapshot(GovernanceHealthSnapshot snapshot) {
        if (snapshotMapper.selectById(snapshot.snapshotId()) == null) {
            snapshotMapper.insert(toEntity(snapshot));
        } else {
            snapshotMapper.updateById(toEntity(snapshot));
        }
        return snapshot;
    }

    @Override
    public Optional<GovernanceAlertRecord> findAlertById(String alertId) {
        return Optional.ofNullable(alertMapper.selectById(alertId)).map(this::toDomain);
    }

    @Override
    public Optional<GovernanceAlertRecord> findOpenAlertByKey(String alertKey) {
        return alertMapper.selectList(new LambdaQueryWrapper<GovernanceAlertRecordEntity>()
                        .eq(GovernanceAlertRecordEntity::getAlertKey, alertKey)
                        .ne(GovernanceAlertRecordEntity::getStatus, AlertStatus.CLOSED.name()))
                .stream()
                .map(this::toDomain)
                .findFirst();
    }

    @Override
    public List<GovernanceAlertRecord> findAlerts(AlertQuery query) {
        return alertMapper.selectList(new QueryWrapper<>()).stream()
                .map(this::toDomain)
                .filter(alert -> query.targetType() == null || alert.targetType() == query.targetType())
                .filter(alert -> query.targetCode() == null || alert.targetCode().equals(query.targetCode()))
                .filter(alert -> query.alertLevel() == null || alert.alertLevel() == query.alertLevel())
                .filter(alert -> query.alertStatus() == null || alert.status() == query.alertStatus())
                .filter(alert -> query.occurredFrom() == null || !alert.occurredAt().isBefore(query.occurredFrom()))
                .filter(alert -> query.occurredTo() == null || !alert.occurredAt().isAfter(query.occurredTo()))
                .sorted(Comparator.comparing(GovernanceAlertRecord::occurredAt).reversed())
                .toList();
    }

    @Override
    public GovernanceAlertRecord saveAlert(GovernanceAlertRecord alertRecord) {
        if (alertMapper.selectById(alertRecord.alertId()) == null) {
            alertMapper.insert(toEntity(alertRecord));
        } else {
            alertMapper.updateById(toEntity(alertRecord));
        }
        return alertRecord;
    }

    @Override
    public Optional<GovernanceTraceRecord> findTraceById(String traceId) {
        return Optional.ofNullable(traceMapper.selectById(traceId)).map(this::toDomain);
    }

    @Override
    public List<GovernanceTraceRecord> findTraces(TraceQuery query) {
        return traceMapper.selectList(new QueryWrapper<>()).stream()
                .map(this::toDomain)
                .filter(trace -> query.targetType() == null || trace.targetType() == query.targetType())
                .filter(trace -> query.targetCode() == null || trace.targetCode().equals(query.targetCode()))
                .filter(trace -> query.traceStatus() == null || trace.status() == query.traceStatus())
                .filter(trace -> query.openedFrom() == null || !trace.openedAt().isBefore(query.openedFrom()))
                .filter(trace -> query.openedTo() == null || !trace.openedAt().isAfter(query.openedTo()))
                .sorted(Comparator.comparing(GovernanceTraceRecord::updatedAt).reversed())
                .toList();
    }

    @Override
    public GovernanceTraceRecord saveTrace(GovernanceTraceRecord traceRecord) {
        if (traceMapper.selectById(traceRecord.traceId()) == null) {
            traceMapper.insert(toEntity(traceRecord));
        } else {
            traceMapper.updateById(toEntity(traceRecord));
        }
        return traceRecord;
    }

    @Override
    public Optional<GovernanceActionAuditRecord> findAuditByRequestId(String requestId) {
        return auditMapper.selectList(new LambdaQueryWrapper<GovernanceActionAuditEntity>()
                        .eq(GovernanceActionAuditEntity::getRequestId, requestId))
                .stream()
                .map(this::toDomain)
                .findFirst();
    }

    @Override
    public List<GovernanceActionAuditRecord> findAudits(AuditQuery query) {
        return auditMapper.selectList(new QueryWrapper<>()).stream()
                .map(this::toDomain)
                .filter(audit -> query.targetType() == null || audit.targetType() == query.targetType())
                .filter(audit -> query.targetCode() == null || audit.targetCode().equals(query.targetCode()))
                .filter(audit -> query.createdFrom() == null || !audit.createdAt().isBefore(query.createdFrom()))
                .filter(audit -> query.createdTo() == null || !audit.createdAt().isAfter(query.createdTo()))
                .sorted(Comparator.comparing(GovernanceActionAuditRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public GovernanceActionAuditRecord saveAudit(GovernanceActionAuditRecord auditRecord) {
        if (auditMapper.selectById(auditRecord.auditId()) == null) {
            auditMapper.insert(toEntity(auditRecord));
        } else {
            auditMapper.updateById(toEntity(auditRecord));
        }
        return auditRecord;
    }

    private GovernanceRuntimeSignal toDomain(GovernanceRuntimeSignalEntity entity) {
        return new GovernanceRuntimeSignal(
                entity.getSignalId(),
                entity.getTenantId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                RuntimeTargetStatus.valueOf(entity.getRuntimeStatus()),
                entity.getTotalExecutions() == null ? 0 : entity.getTotalExecutions(),
                entity.getFailureCount() == null ? 0 : entity.getFailureCount(),
                entity.getFailureRate(),
                entity.getLastDurationMs(),
                entity.getFreshnessLagSeconds(),
                entity.getLastSuccessAt(),
                entity.getLastFailureAt(),
                entity.getLastErrorCode(),
                entity.getLastErrorMessage(),
                entity.getLastEventType(),
                entity.getLastExecutionId(),
                entity.getTraceId(),
                entity.getPayloadJson(),
                entity.getUpdatedAt()
        );
    }

    private GovernanceHealthSnapshot toDomain(GovernanceHealthSnapshotEntity entity) {
        return new GovernanceHealthSnapshot(
                entity.getSnapshotId(),
                entity.getGovernanceId(),
                entity.getRuleId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                entity.getRuleCode(),
                GovernanceHealthStatus.valueOf(entity.getHealthStatus()),
                entity.getMeasuredValue(),
                entity.getThresholdValue(),
                entity.getSummary(),
                entity.getTraceId(),
                entity.getCheckedAt()
        );
    }

    private GovernanceAlertRecord toDomain(GovernanceAlertRecordEntity entity) {
        return new GovernanceAlertRecord(
                entity.getAlertId(),
                entity.getGovernanceId(),
                entity.getRuleId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                AlertLevel.valueOf(entity.getAlertLevel()),
                entity.getAlertType(),
                AlertStatus.valueOf(entity.getStatus()),
                entity.getAlertKey(),
                entity.getSummary(),
                entity.getDetail(),
                entity.getTraceId(),
                entity.getOccurredAt(),
                entity.getAcknowledgedAt(),
                entity.getAcknowledgedBy(),
                entity.getEscalatedAt(),
                entity.getEscalatedBy(),
                entity.getClosedAt(),
                entity.getClosedBy(),
                entity.getCloseReason()
        );
    }

    private GovernanceTraceRecord toDomain(GovernanceTraceRecordEntity entity) {
        return new GovernanceTraceRecord(
                entity.getTraceId(),
                entity.getGovernanceId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                GovernanceTraceType.valueOf(entity.getTraceType()),
                TraceStatus.valueOf(entity.getStatus()),
                entity.getSourceEventType(),
                entity.getSourceExecutionId(),
                entity.getCorrelationId(),
                entity.getSummary(),
                entity.getDetail(),
                entity.getOpenedAt(),
                entity.getUpdatedAt(),
                entity.getResolvedAt()
        );
    }

    private GovernanceActionAuditRecord toDomain(GovernanceActionAuditEntity entity) {
        return new GovernanceActionAuditRecord(
                entity.getAuditId(),
                entity.getGovernanceId(),
                GovernanceScopeType.valueOf(entity.getTargetType()),
                entity.getTargetCode(),
                GovernanceActionType.valueOf(entity.getActionType()),
                GovernanceActionResult.valueOf(entity.getActionResult()),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getReason(),
                entity.getRequestId(),
                entity.getPayloadJson(),
                entity.getResultMessage(),
                entity.getTraceId(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private GovernanceRuntimeSignalEntity toEntity(GovernanceRuntimeSignal signal) {
        return new GovernanceRuntimeSignalEntity(
                signal.signalId(),
                signal.tenantId(),
                signal.targetType().name(),
                signal.targetCode(),
                signal.runtimeStatus().name(),
                signal.totalExecutions(),
                signal.failureCount(),
                signal.failureRate(),
                signal.lastDurationMs(),
                signal.freshnessLagSeconds(),
                signal.lastSuccessAt(),
                signal.lastFailureAt(),
                signal.lastErrorCode(),
                signal.lastErrorMessage(),
                signal.lastEventType(),
                signal.lastExecutionId(),
                signal.traceId(),
                signal.payloadJson(),
                0L,
                0,
                signal.updatedAt(),
                signal.updatedAt()
        );
    }

    private GovernanceHealthSnapshotEntity toEntity(GovernanceHealthSnapshot snapshot) {
        return new GovernanceHealthSnapshotEntity(
                snapshot.snapshotId(),
                snapshot.governanceId(),
                snapshot.ruleId(),
                snapshot.targetType().name(),
                snapshot.targetCode(),
                snapshot.ruleCode(),
                snapshot.healthStatus().name(),
                snapshot.measuredValue(),
                snapshot.thresholdValue(),
                snapshot.summary(),
                snapshot.traceId(),
                snapshot.checkedAt(),
                0
        );
    }

    private GovernanceAlertRecordEntity toEntity(GovernanceAlertRecord alertRecord) {
        return new GovernanceAlertRecordEntity(
                alertRecord.alertId(),
                alertRecord.governanceId(),
                alertRecord.ruleId(),
                alertRecord.targetType().name(),
                alertRecord.targetCode(),
                alertRecord.alertLevel().name(),
                alertRecord.alertType(),
                alertRecord.status().name(),
                alertRecord.alertKey(),
                alertRecord.summary(),
                alertRecord.detail(),
                alertRecord.traceId(),
                alertRecord.occurredAt(),
                alertRecord.acknowledgedAt(),
                alertRecord.acknowledgedBy(),
                alertRecord.escalatedAt(),
                alertRecord.escalatedBy(),
                alertRecord.closedAt(),
                alertRecord.closedBy(),
                alertRecord.closeReason(),
                0
        );
    }

    private GovernanceTraceRecordEntity toEntity(GovernanceTraceRecord traceRecord) {
        return new GovernanceTraceRecordEntity(
                traceRecord.traceId(),
                traceRecord.governanceId(),
                traceRecord.targetType().name(),
                traceRecord.targetCode(),
                traceRecord.traceType().name(),
                traceRecord.status().name(),
                traceRecord.sourceEventType(),
                traceRecord.sourceExecutionId(),
                traceRecord.correlationId(),
                traceRecord.summary(),
                traceRecord.detail(),
                traceRecord.openedAt(),
                traceRecord.updatedAt(),
                traceRecord.resolvedAt(),
                0
        );
    }

    private GovernanceActionAuditEntity toEntity(GovernanceActionAuditRecord auditRecord) {
        return new GovernanceActionAuditEntity(
                auditRecord.auditId(),
                auditRecord.governanceId(),
                auditRecord.targetType().name(),
                auditRecord.targetCode(),
                auditRecord.actionType().name(),
                auditRecord.actionResult().name(),
                auditRecord.operatorId(),
                auditRecord.operatorName(),
                auditRecord.reason(),
                auditRecord.requestId(),
                auditRecord.payloadJson(),
                auditRecord.resultMessage(),
                auditRecord.traceId(),
                auditRecord.createdAt(),
                auditRecord.completedAt(),
                0
        );
    }
}
