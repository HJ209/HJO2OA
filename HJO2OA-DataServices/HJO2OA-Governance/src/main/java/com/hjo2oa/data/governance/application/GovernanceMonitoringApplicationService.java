package com.hjo2oa.data.governance.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.governance.application.GovernanceCommands.AlertActionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.GovernancePagedResult;
import com.hjo2oa.data.governance.application.GovernanceCommands.ManualGovernanceInterventionCommand;
import com.hjo2oa.data.governance.application.GovernanceCommands.RunHealthCheckCommand;
import com.hjo2oa.data.governance.domain.AlertRule;
import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiDeprecatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiPublishedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataConnectorUpdatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataGovernanceAlertedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataReportRefreshedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataServiceActivatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncCompletedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncFailedEvent;
import com.hjo2oa.data.governance.domain.GovernanceHealthSnapshot;
import com.hjo2oa.data.governance.domain.GovernanceProfile;
import com.hjo2oa.data.governance.domain.GovernanceProfileRepository;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.HealthSnapshotQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeRepository;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeSignal;
import com.hjo2oa.data.governance.domain.GovernanceTraceRecord;
import com.hjo2oa.data.governance.domain.HealthCheckRule;
import com.hjo2oa.data.governance.domain.ServiceVersionRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ComparisonOperator;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionResult;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceHealthStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceTraceType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckSeverity;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GovernanceMonitoringApplicationService {

    private final GovernanceProfileRepository profileRepository;
    private final GovernanceRuntimeRepository runtimeRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    @Autowired
    public GovernanceMonitoringApplicationService(
            GovernanceProfileRepository profileRepository,
            GovernanceRuntimeRepository runtimeRepository,
            DomainEventPublisher domainEventPublisher,
            ObjectMapper objectMapper
    ) {
        this(profileRepository, runtimeRepository, domainEventPublisher, objectMapper, Clock.systemUTC());
    }
    public GovernanceMonitoringApplicationService(
            GovernanceProfileRepository profileRepository,
            GovernanceRuntimeRepository runtimeRepository,
            DomainEventPublisher domainEventPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.runtimeRepository = Objects.requireNonNull(runtimeRepository, "runtimeRepository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void handleDataApiPublished(DataApiPublishedEvent event) {
        GovernanceRuntimeSignal signal = upsertSignal(
                event.tenantId(),
                GovernanceScopeType.API,
                event.code(),
                existing -> existing.updateLifecycle(RuntimeTargetStatus.ACTIVE, event.eventType(), payload(event), now())
                        .updateFreshness(event.eventType(), event.occurredAt(), now()),
                now()
        );
        profileRepository.findByTarget(event.tenantId(), GovernanceScopeType.API, event.code())
                .ifPresent(profile -> {
                    registerOrPublishVersion(profile, event.version(), "Upstream API published");
                    resolveTraceIfRecovered(profile, signal, "API published", payload(event));
                });
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.API, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataApiDeprecated(DataApiDeprecatedEvent event) {
        GovernanceRuntimeSignal signal = upsertSignal(
                event.tenantId(),
                GovernanceScopeType.API,
                event.code(),
                existing -> existing.updateLifecycle(RuntimeTargetStatus.DEGRADED, event.eventType(), payload(event), now())
                        .updateFreshness(event.eventType(), event.occurredAt(), now()),
                now()
        );
        profileRepository.findByTarget(event.tenantId(), GovernanceScopeType.API, event.code())
                .ifPresent(profile -> {
                    deprecateVersion(profile, event.version(), "Upstream API deprecated");
                    resolveTrace(profile, signal.traceId(), TraceStatus.INVESTIGATING, "API deprecated", payload(event));
                });
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.API, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataConnectorUpdated(DataConnectorUpdatedEvent event) {
        upsertSignal(
                event.tenantId(),
                GovernanceScopeType.CONNECTOR,
                event.code(),
                existing -> existing.updateLifecycle(
                        event.status() == null ? RuntimeTargetStatus.UNKNOWN : event.status(),
                        event.eventType(),
                        payload(event),
                        now()
                ),
                now()
        );
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.CONNECTOR, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataSyncCompleted(DataSyncCompletedEvent event) {
        GovernanceRuntimeSignal signal = upsertSignal(
                event.tenantId(),
                GovernanceScopeType.SYNC,
                event.code(),
                existing -> existing.markSuccess(
                        event.eventType(),
                        event.executionId(),
                        null,
                        event.occurredAt(),
                        payload(event),
                        now()
                ),
                now()
        );
        profileRepository.findByTarget(event.tenantId(), GovernanceScopeType.SYNC, event.code())
                .ifPresent(profile -> resolveTraceIfRecovered(profile, signal, "Sync completed", payload(event)));
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.SYNC, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataSyncFailed(DataSyncFailedEvent event) {
        String traceId = UUID.randomUUID().toString();
        GovernanceRuntimeSignal signal = upsertSignal(
                event.tenantId(),
                GovernanceScopeType.SYNC,
                event.code(),
                existing -> existing.markFailure(
                        event.eventType(),
                        event.executionId(),
                        event.errorCode(),
                        event.errorMessage(),
                        traceId,
                        payload(event),
                        now()
                ),
                now()
        );
        profileRepository.findByTarget(event.tenantId(), GovernanceScopeType.SYNC, event.code())
                .ifPresent(profile -> resolveTrace(profile, traceId, TraceStatus.OPEN, "Sync failed", payload(event)));
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.SYNC, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataReportRefreshed(DataReportRefreshedEvent event) {
        upsertSignal(
                event.tenantId(),
                GovernanceScopeType.REPORT,
                event.code(),
                existing -> existing.markSuccess(
                        event.eventType(),
                        null,
                        null,
                        event.snapshotAt(),
                        payload(event),
                        now()
                ),
                now()
        );
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.REPORT, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public void handleDataServiceActivated(DataServiceActivatedEvent event) {
        upsertSignal(
                event.tenantId(),
                GovernanceScopeType.MODULE,
                event.code(),
                existing -> existing.updateLifecycle(RuntimeTargetStatus.ACTIVE, event.eventType(), payload(event), now())
                        .updateFreshness(event.eventType(), event.occurredAt(), now()),
                now()
        );
        runHealthChecksInternal(event.tenantId(), GovernanceScopeType.MODULE, event.code(), "system", "system", UUID.randomUUID().toString());
    }

    public GovernancePagedResult<GovernanceHealthSnapshot> runHealthChecks(RunHealthCheckCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        List<GovernanceHealthSnapshot> items = runHealthChecksInternal(
                command.tenantId(),
                command.targetType(),
                command.targetCode(),
                command.operatorId(),
                command.operatorName(),
                command.requestId()
        );
        return new GovernancePagedResult<>(items, items.size());
    }

    public GovernancePagedResult<GovernanceHealthSnapshot> runScheduledHealthChecks() {
        List<GovernanceHealthSnapshot> items = runHealthChecksInternal(
                null,
                null,
                null,
                "system",
                "system",
                UUID.randomUUID().toString()
        );
        return new GovernancePagedResult<>(items, items.size());
    }

    public GovernancePagedResult<GovernanceHealthSnapshot> listSnapshots(HealthSnapshotQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<GovernanceHealthSnapshot> items = runtimeRepository.findSnapshots(query).stream()
                .sorted(Comparator.comparing(GovernanceHealthSnapshot::checkedAt).reversed())
                .toList();
        return new GovernancePagedResult<>(items, items.size());
    }

    public GovernancePagedResult<GovernanceAlertRecord> listAlerts(AlertQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<GovernanceAlertRecord> items = runtimeRepository.findAlerts(query).stream()
                .sorted(Comparator.comparing(GovernanceAlertRecord::occurredAt).reversed())
                .toList();
        return new GovernancePagedResult<>(items, items.size());
    }

    public GovernanceAlertRecord getAlert(String alertId) {
        return runtimeRepository.findAlertById(alertId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Alert record not found"));
    }

    public GovernanceAlertRecord handleAlertAction(AlertActionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GovernanceAlertRecord existing = getAlert(command.alertId());
        Optional<GovernanceActionAuditRecord> idempotentAudit = runtimeRepository.findAuditByRequestId(resolveRequestId(command.requestId()));
        if (idempotentAudit.isPresent()) {
            return runtimeRepository.findAlertById(command.alertId()).orElse(existing);
        }

        GovernanceAlertRecord updated = switch (command.actionType()) {
            case ACKNOWLEDGE_ALERT -> acknowledgeAlert(existing, command);
            case ESCALATE_ALERT -> escalateAlert(existing, command);
            case CLOSE_ALERT -> closeAlert(existing, command);
            default -> throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Unsupported alert action: " + command.actionType()
            );
        };

        runtimeRepository.saveAlert(updated);
        updateTraceForAlert(updated);
        runtimeRepository.saveAudit(new GovernanceActionAuditRecord(
                UUID.randomUUID().toString(),
                existing.governanceId(),
                existing.targetType(),
                existing.targetCode(),
                command.actionType(),
                GovernanceActionResult.COMPLETED,
                normalizeOperator(command.operatorId()),
                command.operatorName(),
                command.reason(),
                resolveRequestId(command.requestId()),
                updated.alertId(),
                updated.status().name(),
                updated.traceId(),
                now(),
                now()
        ));
        return updated;
    }

    public GovernanceActionAuditRecord submitIntervention(ManualGovernanceInterventionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requestId = resolveRequestId(command.requestId());
        Optional<GovernanceActionAuditRecord> existingAudit = runtimeRepository.findAuditByRequestId(requestId);
        if (existingAudit.isPresent()) {
            return existingAudit.orElseThrow();
        }

        GovernanceProfile profile = profileRepository.findByTarget(
                        command.tenantId(),
                        command.targetType(),
                        command.targetCode()
                )
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Governance profile not found for target"
                ));

        if (command.actionType() != GovernanceActionType.ADD_NOTE
                && !isActionAllowed(profile.alertPolicyJson(), command.actionType())) {
            GovernanceActionAuditRecord rejected = runtimeRepository.saveAudit(new GovernanceActionAuditRecord(
                    UUID.randomUUID().toString(),
                    profile.governanceId(),
                    profile.scopeType(),
                    profile.targetCode(),
                    command.actionType(),
                    GovernanceActionResult.REJECTED,
                    normalizeOperator(command.operatorId()),
                    command.operatorName(),
                    command.reason(),
                    requestId,
                    command.payloadJson(),
                    "Strategy denied",
                    command.traceId(),
                    now(),
                    now()
            ));
            throw new BizException(GovernanceErrorDescriptors.STRATEGY_NOT_ALLOWED, rejected.resultMessage());
        }

        GovernanceActionAuditRecord accepted = runtimeRepository.saveAudit(new GovernanceActionAuditRecord(
                UUID.randomUUID().toString(),
                profile.governanceId(),
                profile.scopeType(),
                profile.targetCode(),
                command.actionType(),
                GovernanceActionResult.ACCEPTED,
                normalizeOperator(command.operatorId()),
                command.operatorName(),
                command.reason(),
                requestId,
                command.payloadJson(),
                "Intervention accepted through governance entry",
                command.traceId(),
                now(),
                now()
        ));
        if (command.traceId() != null && !command.traceId().isBlank()) {
            GovernanceTraceRecord trace = runtimeRepository.findTraceById(command.traceId())
                    .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Trace record not found"));
            TraceStatus nextStatus = command.actionType() == GovernanceActionType.REQUEST_COMPENSATION
                    ? TraceStatus.COMPENSATED
                    : TraceStatus.INVESTIGATING;
            runtimeRepository.saveTrace(trace.updateStatus(
                    nextStatus,
                    trace.summary(),
                    appendTraceDetail(trace.detail(), command.reason()),
                    now(),
                    nextStatus == TraceStatus.COMPENSATED ? now() : null
            ));
        }
        return accepted;
    }

    public GovernancePagedResult<GovernanceTraceRecord> listTraces(TraceQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<GovernanceTraceRecord> items = runtimeRepository.findTraces(query).stream()
                .sorted(Comparator.comparing(GovernanceTraceRecord::updatedAt).reversed())
                .toList();
        return new GovernancePagedResult<>(items, items.size());
    }

    public GovernancePagedResult<GovernanceActionAuditRecord> listAudits(AuditQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<GovernanceActionAuditRecord> items = runtimeRepository.findAudits(query).stream()
                .sorted(Comparator.comparing(GovernanceActionAuditRecord::createdAt).reversed())
                .toList();
        return new GovernancePagedResult<>(items, items.size());
    }

    private List<GovernanceHealthSnapshot> runHealthChecksInternal(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            String operatorId,
            String operatorName,
            String requestId
    ) {
        List<GovernanceHealthSnapshot> allSnapshots = new ArrayList<>();
        List<GovernanceProfile> profiles = profileRepository.findAllActive().stream()
                .filter(profile -> tenantId == null || profile.tenantId().equals(tenantId))
                .filter(profile -> targetType == null || profile.scopeType() == targetType)
                .filter(profile -> targetCode == null || profile.targetCode().equals(targetCode))
                .toList();

        for (GovernanceProfile profile : profiles) {
            GovernanceRuntimeSignal signal = runtimeRepository.findSignal(
                            profile.tenantId(),
                            profile.scopeType(),
                            profile.targetCode()
                    )
                    .orElse(null);
            Map<String, GovernanceHealthSnapshot> snapshotByRuleCode = new HashMap<>();
            for (HealthCheckRule rule : profile.healthCheckRules()) {
                if (rule.status() != HealthCheckRuleStatus.ENABLED) {
                    continue;
                }
                GovernanceHealthSnapshot snapshot = evaluateRule(profile, rule, signal, requestId);
                runtimeRepository.saveSnapshot(snapshot);
                snapshotByRuleCode.put(rule.ruleCode(), snapshot);
                allSnapshots.add(snapshot);
            }
            triggerAlerts(profile, signal, snapshotByRuleCode);
            runtimeRepository.saveAudit(new GovernanceActionAuditRecord(
                    UUID.randomUUID().toString(),
                    profile.governanceId(),
                    profile.scopeType(),
                    profile.targetCode(),
                    GovernanceActionType.RUN_HEALTH_CHECK,
                    GovernanceActionResult.COMPLETED,
                    normalizeOperator(operatorId),
                    operatorName,
                    "Health check executed",
                    resolveRequestId(requestId),
                    Integer.toString(snapshotByRuleCode.size()),
                    "OK",
                    signal == null ? null : signal.traceId(),
                    now(),
                    now()
            ));
        }
        return List.copyOf(allSnapshots);
    }

    private GovernanceHealthSnapshot evaluateRule(
            GovernanceProfile profile,
            HealthCheckRule rule,
            GovernanceRuntimeSignal signal,
            String requestId
    ) {
        Instant now = now();
        BigDecimal measuredValue = resolveMetric(signal, rule.metricName(), now);
        GovernanceHealthStatus healthStatus;
        String summary;
        String traceId = signal == null ? null : signal.traceId();
        if (measuredValue == null) {
            healthStatus = GovernanceHealthStatus.UNKNOWN;
            measuredValue = BigDecimal.ZERO;
            summary = "Metric unavailable: " + rule.metricName();
        } else if (matches(rule.comparisonOperator(), measuredValue, rule.thresholdValue())) {
            healthStatus = toHealthStatus(rule.severity());
            summary = "Rule triggered for metric " + rule.metricName();
        } else {
            healthStatus = GovernanceHealthStatus.HEALTHY;
            summary = "Health check passed for metric " + rule.metricName();
        }
        return new GovernanceHealthSnapshot(
                UUID.randomUUID().toString(),
                profile.governanceId(),
                rule.ruleId(),
                profile.scopeType(),
                profile.targetCode(),
                rule.ruleCode(),
                healthStatus,
                measuredValue,
                rule.thresholdValue(),
                summary,
                traceId,
                now
        );
    }

    private void triggerAlerts(
            GovernanceProfile profile,
            GovernanceRuntimeSignal signal,
            Map<String, GovernanceHealthSnapshot> snapshotByRuleCode
    ) {
        Instant now = now();
        for (AlertRule alertRule : profile.alertRules()) {
            if (alertRule.status() != com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus.ENABLED) {
                continue;
            }
            GovernanceHealthSnapshot sourceSnapshot = alertRule.sourceRuleCode() == null
                    ? null
                    : snapshotByRuleCode.get(alertRule.sourceRuleCode());
            BigDecimal measuredValue = sourceSnapshot == null
                    ? resolveMetric(signal, alertRule.metricName(), now)
                    : sourceSnapshot.measuredValue();
            boolean triggered = sourceSnapshot != null
                    ? sourceSnapshot.healthStatus() == GovernanceHealthStatus.UNHEALTHY
                    || sourceSnapshot.healthStatus() == GovernanceHealthStatus.DEGRADED
                    : measuredValue != null && matches(alertRule.comparisonOperator(), measuredValue, alertRule.thresholdValue());
            if (!triggered) {
                continue;
            }
            String alertKey = buildAlertKey(profile, alertRule, now);
            if (runtimeRepository.findOpenAlertByKey(alertKey).isPresent()) {
                continue;
            }
            String traceId = signal != null && signal.traceId() != null ? signal.traceId() : UUID.randomUUID().toString();
            GovernanceAlertRecord alertRecord = new GovernanceAlertRecord(
                    UUID.randomUUID().toString(),
                    profile.governanceId(),
                    alertRule.ruleId(),
                    profile.scopeType(),
                    profile.targetCode(),
                    alertRule.alertLevel(),
                    alertRule.alertType(),
                    AlertStatus.OPEN,
                    alertKey,
                    "Governance alert triggered: " + alertRule.ruleCode(),
                    buildAlertDetail(signal, sourceSnapshot),
                    traceId,
                    now,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            runtimeRepository.saveAlert(alertRecord);
            resolveTrace(profile, traceId, TraceStatus.OPEN, alertRecord.summary(), alertRecord.detail());
            publishAlert(profile, alertRecord);
        }
    }

    private void publishAlert(GovernanceProfile profile, GovernanceAlertRecord alertRecord) {
        domainEventPublisher.publish(new DataGovernanceAlertedEvent(
                UUID.randomUUID(),
                now(),
                profile.tenantId(),
                profile.governanceId(),
                alertRecord.alertId(),
                alertRecord.targetCode(),
                alertRecord.targetType(),
                alertRecord.alertLevel(),
                alertRecord.alertType()
        ));
    }

    private GovernanceRuntimeSignal upsertSignal(
            String tenantId,
            GovernanceScopeType targetType,
            String targetCode,
            java.util.function.Function<GovernanceRuntimeSignal, GovernanceRuntimeSignal> updater,
            Instant now
    ) {
        GovernanceRuntimeSignal base = runtimeRepository.findSignal(tenantId, targetType, targetCode)
                .orElseGet(() -> GovernanceRuntimeSignal.initialize(UUID.randomUUID().toString(), tenantId, targetType, targetCode, now));
        GovernanceRuntimeSignal saved = updater.apply(base);
        runtimeRepository.saveSignal(saved);
        return saved;
    }

    private GovernanceAlertRecord acknowledgeAlert(GovernanceAlertRecord alertRecord, AlertActionCommand command) {
        if (alertRecord.status() == AlertStatus.ACKNOWLEDGED) {
            return alertRecord;
        }
        if (alertRecord.status() != AlertStatus.OPEN) {
            throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Only open alert can be acknowledged"
            );
        }
        return alertRecord.acknowledge(normalizeOperator(command.operatorId()), now());
    }

    private GovernanceAlertRecord escalateAlert(GovernanceAlertRecord alertRecord, AlertActionCommand command) {
        if (alertRecord.status() == AlertStatus.ESCALATED) {
            return alertRecord;
        }
        if (alertRecord.status() != AlertStatus.OPEN && alertRecord.status() != AlertStatus.ACKNOWLEDGED) {
            throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Only open or acknowledged alert can be escalated"
            );
        }
        return alertRecord.escalate(normalizeOperator(command.operatorId()), now());
    }

    private GovernanceAlertRecord closeAlert(GovernanceAlertRecord alertRecord, AlertActionCommand command) {
        if (alertRecord.status() == AlertStatus.CLOSED) {
            return alertRecord;
        }
        if (alertRecord.status() != AlertStatus.ACKNOWLEDGED && alertRecord.status() != AlertStatus.ESCALATED) {
            throw new BizException(
                    GovernanceErrorDescriptors.INVALID_STATUS_TRANSITION,
                    "Only acknowledged or escalated alert can be closed"
            );
        }
        return alertRecord.close(normalizeOperator(command.operatorId()), command.reason(), now());
    }

    private void updateTraceForAlert(GovernanceAlertRecord alertRecord) {
        if (alertRecord.traceId() == null || alertRecord.traceId().isBlank()) {
            return;
        }
        runtimeRepository.findTraceById(alertRecord.traceId()).ifPresent(trace -> runtimeRepository.saveTrace(trace.updateStatus(
                alertRecord.status() == AlertStatus.CLOSED ? TraceStatus.RESOLVED : TraceStatus.INVESTIGATING,
                trace.summary(),
                appendTraceDetail(trace.detail(), "Alert status changed to " + alertRecord.status().name()),
                now(),
                alertRecord.status() == AlertStatus.CLOSED ? now() : null
        )));
    }

    private void resolveTraceIfRecovered(
            GovernanceProfile profile,
            GovernanceRuntimeSignal signal,
            String summary,
            String detail
    ) {
        if (signal.traceId() == null || signal.traceId().isBlank()) {
            return;
        }
        resolveTrace(profile, signal.traceId(), TraceStatus.RESOLVED, summary, detail);
    }

    private void resolveTrace(
            GovernanceProfile profile,
            String traceId,
            TraceStatus traceStatus,
            String summary,
            String detail
    ) {
        if (traceId == null || traceId.isBlank()) {
            return;
        }
        GovernanceTraceRecord traceRecord = runtimeRepository.findTraceById(traceId)
                .orElse(new GovernanceTraceRecord(
                        traceId,
                        profile.governanceId(),
                        profile.scopeType(),
                        profile.targetCode(),
                        traceStatus == TraceStatus.RESOLVED ? GovernanceTraceType.PUBLICATION : GovernanceTraceType.ALERT,
                        traceStatus,
                        null,
                        null,
                        null,
                        summary,
                        detail,
                        now(),
                        now(),
                        traceStatus == TraceStatus.RESOLVED ? now() : null
                ));
        runtimeRepository.saveTrace(traceRecord.updateStatus(
                traceStatus,
                summary,
                detail,
                now(),
                traceStatus == TraceStatus.RESOLVED || traceStatus == TraceStatus.COMPENSATED ? now() : null
        ));
    }

    private void registerOrPublishVersion(GovernanceProfile profile, String version, String changeSummary) {
        List<ServiceVersionRecord> records = new ArrayList<>(profile.serviceVersionRecords());
        Optional<ServiceVersionRecord> existing = records.stream()
                .filter(record -> record.version().equals(version))
                .findFirst();
        if (existing.isPresent() && existing.orElseThrow().status() == com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus.PUBLISHED) {
            return;
        }
        ServiceVersionRecord nextRecord = existing
                .map(record -> record.publish("system", changeSummary, UUID.randomUUID().toString(), now()))
                .orElseGet(() -> ServiceVersionRecord.register(
                        UUID.randomUUID().toString(),
                        profile.governanceId(),
                        profile.scopeType(),
                        profile.targetCode(),
                        version,
                        null,
                        changeSummary,
                        "system",
                        changeSummary,
                        UUID.randomUUID().toString(),
                        now()
                ).publish("system", changeSummary, UUID.randomUUID().toString(), now()));
        replaceVersionRecord(records, nextRecord);
        profileRepository.save(profile.replaceServiceVersionRecords(records, now()));
    }

    private void deprecateVersion(GovernanceProfile profile, String version, String changeSummary) {
        List<ServiceVersionRecord> records = new ArrayList<>(profile.serviceVersionRecords());
        Optional<ServiceVersionRecord> existing = records.stream()
                .filter(record -> record.version().equals(version))
                .findFirst();
        if (existing.isEmpty()) {
            return;
        }
        ServiceVersionRecord nextRecord = existing.orElseThrow().deprecate("system", changeSummary, UUID.randomUUID().toString(), now());
        replaceVersionRecord(records, nextRecord);
        profileRepository.save(profile.replaceServiceVersionRecords(records, now()));
    }

    private void replaceVersionRecord(List<ServiceVersionRecord> records, ServiceVersionRecord nextRecord) {
        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).version().equals(nextRecord.version())) {
                records.set(index, nextRecord);
                return;
            }
        }
        records.add(nextRecord);
    }

    private BigDecimal resolveMetric(GovernanceRuntimeSignal signal, String metricName, Instant now) {
        if (signal == null || metricName == null || metricName.isBlank()) {
            return null;
        }
        return switch (metricName.trim().toUpperCase()) {
            case "FAILURE_COUNT" -> BigDecimal.valueOf(signal.failureCount());
            case "FAILURE_RATE" -> signal.failureRate();
            case "TOTAL_EXECUTIONS" -> BigDecimal.valueOf(signal.totalExecutions());
            case "LAST_DURATION_MS" -> signal.lastDurationMs() == null ? null : BigDecimal.valueOf(signal.lastDurationMs());
            case "FRESHNESS_LAG_SECONDS" -> {
                if (signal.freshnessLagSeconds() != null) {
                    yield BigDecimal.valueOf(signal.freshnessLagSeconds());
                }
                if (signal.lastSuccessAt() == null) {
                    yield null;
                }
                yield BigDecimal.valueOf(Math.max(0, Duration.between(signal.lastSuccessAt(), now).getSeconds()));
            }
            case "STATUS_FAILED" -> BigDecimal.valueOf(signal.runtimeStatus() == RuntimeTargetStatus.FAILED ? 1 : 0);
            case "STATUS_DEGRADED" -> BigDecimal.valueOf(signal.runtimeStatus() == RuntimeTargetStatus.DEGRADED ? 1 : 0);
            default -> resolveMetricFromPayload(signal.payloadJson(), metricName);
        };
    }

    private BigDecimal resolveMetricFromPayload(String payloadJson, String metricName) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode valueNode = root.get(metricName);
            if (valueNode == null || !valueNode.isNumber()) {
                return null;
            }
            return valueNode.decimalValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean matches(ComparisonOperator operator, BigDecimal measuredValue, BigDecimal thresholdValue) {
        return switch (operator) {
            case GREATER_THAN -> measuredValue.compareTo(thresholdValue) > 0;
            case GREATER_OR_EQUAL -> measuredValue.compareTo(thresholdValue) >= 0;
            case LESS_THAN -> measuredValue.compareTo(thresholdValue) < 0;
            case LESS_OR_EQUAL -> measuredValue.compareTo(thresholdValue) <= 0;
            case EQUAL -> measuredValue.compareTo(thresholdValue) == 0;
        };
    }

    private GovernanceHealthStatus toHealthStatus(HealthCheckSeverity severity) {
        return severity == HealthCheckSeverity.INFO || severity == HealthCheckSeverity.WARN
                ? GovernanceHealthStatus.DEGRADED
                : GovernanceHealthStatus.UNHEALTHY;
    }

    private boolean isActionAllowed(String alertPolicyJson, GovernanceActionType actionType) {
        if (alertPolicyJson == null || alertPolicyJson.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(alertPolicyJson);
            JsonNode allowedActions = root.get("allowedActions");
            if (allowedActions == null || !allowedActions.isArray()) {
                return false;
            }
            for (JsonNode actionNode : allowedActions) {
                if (actionNode.isTextual() && actionType.name().equalsIgnoreCase(actionNode.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private String buildAlertKey(GovernanceProfile profile, AlertRule alertRule, Instant now) {
        int dedupMinutes = alertRule.dedupMinutes() == null || alertRule.dedupMinutes() <= 0 ? 1 : alertRule.dedupMinutes();
        long bucket = now.getEpochSecond() / (dedupMinutes * 60L);
        return profile.governanceId() + ":" + profile.targetCode() + ":" + alertRule.ruleCode() + ":" + bucket;
    }

    private String buildAlertDetail(GovernanceRuntimeSignal signal, GovernanceHealthSnapshot sourceSnapshot) {
        if (sourceSnapshot != null) {
            return sourceSnapshot.summary();
        }
        if (signal == null) {
            return "No runtime signal";
        }
        return signal.lastErrorMessage() == null ? "Runtime signal triggered alert" : signal.lastErrorMessage();
    }

    private String payload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            return "{\"serialization\":\"failed\"}";
        }
    }

    private String appendTraceDetail(String originalDetail, String extraDetail) {
        if (extraDetail == null || extraDetail.isBlank()) {
            return originalDetail;
        }
        if (originalDetail == null || originalDetail.isBlank()) {
            return extraDetail;
        }
        return originalDetail + "\n" + extraDetail;
    }

    private String resolveRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim();
    }

    private String normalizeOperator(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? "system" : operatorId.trim();
    }

    private Instant now() {
        return clock.instant();
    }
}
