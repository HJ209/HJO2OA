package com.hjo2oa.data.governance.infrastructure;

import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceHealthSnapshot;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.HealthSnapshotQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeRepository;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeSignal;
import com.hjo2oa.data.governance.domain.GovernanceTraceRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryGovernanceRuntimeRepository implements GovernanceRuntimeRepository {

    private final Map<String, GovernanceRuntimeSignal> signals = new ConcurrentHashMap<>();
    private final Map<String, GovernanceHealthSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, GovernanceAlertRecord> alerts = new ConcurrentHashMap<>();
    private final Map<String, GovernanceTraceRecord> traces = new ConcurrentHashMap<>();
    private final Map<String, GovernanceActionAuditRecord> audits = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<String> signalOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<String> snapshotOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<String> alertOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<String> traceOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<String> auditOrder = new ConcurrentLinkedDeque<>();

    @Override
    public Optional<GovernanceRuntimeSignal> findSignal(String tenantId, GovernanceScopeType targetType, String targetCode) {
        return orderedValues(signalOrder, signals).stream()
                .filter(signal -> signal.tenantId().equals(tenantId))
                .filter(signal -> signal.targetType() == targetType)
                .filter(signal -> signal.targetCode().equals(targetCode))
                .findFirst();
    }

    @Override
    public List<GovernanceRuntimeSignal> findAllSignals() {
        return orderedValues(signalOrder, signals).stream()
                .sorted(Comparator.comparing(GovernanceRuntimeSignal::updatedAt).reversed())
                .toList();
    }

    @Override
    public GovernanceRuntimeSignal saveSignal(GovernanceRuntimeSignal signal) {
        signals.put(signal.signalId(), signal);
        updateRecency(signalOrder, signal.signalId());
        return signal;
    }

    @Override
    public Optional<GovernanceHealthSnapshot> findLatestSnapshot(String governanceId, String ruleId) {
        return orderedValues(snapshotOrder, snapshots).stream()
                .filter(snapshot -> snapshot.governanceId().equals(governanceId))
                .filter(snapshot -> snapshot.ruleId().equals(ruleId))
                .max(Comparator.comparing(GovernanceHealthSnapshot::checkedAt));
    }

    @Override
    public List<GovernanceHealthSnapshot> findSnapshots(HealthSnapshotQuery query) {
        return orderedValues(snapshotOrder, snapshots).stream()
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
        snapshots.put(snapshot.snapshotId(), snapshot);
        updateRecency(snapshotOrder, snapshot.snapshotId());
        return snapshot;
    }

    @Override
    public Optional<GovernanceAlertRecord> findAlertById(String alertId) {
        return Optional.ofNullable(alerts.get(alertId));
    }

    @Override
    public Optional<GovernanceAlertRecord> findOpenAlertByKey(String alertKey) {
        return orderedValues(alertOrder, alerts).stream()
                .filter(alert -> alert.alertKey().equals(alertKey))
                .filter(alert -> alert.status() != AlertStatus.CLOSED)
                .findFirst();
    }

    @Override
    public List<GovernanceAlertRecord> findAlerts(AlertQuery query) {
        return orderedValues(alertOrder, alerts).stream()
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
        alerts.put(alertRecord.alertId(), alertRecord);
        updateRecency(alertOrder, alertRecord.alertId());
        return alertRecord;
    }

    @Override
    public Optional<GovernanceTraceRecord> findTraceById(String traceId) {
        return Optional.ofNullable(traces.get(traceId));
    }

    @Override
    public List<GovernanceTraceRecord> findTraces(TraceQuery query) {
        return orderedValues(traceOrder, traces).stream()
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
        traces.put(traceRecord.traceId(), traceRecord);
        updateRecency(traceOrder, traceRecord.traceId());
        return traceRecord;
    }

    @Override
    public Optional<GovernanceActionAuditRecord> findAuditByRequestId(String requestId) {
        return orderedValues(auditOrder, audits).stream()
                .filter(audit -> audit.requestId().equals(requestId))
                .findFirst();
    }

    @Override
    public List<GovernanceActionAuditRecord> findAudits(AuditQuery query) {
        return orderedValues(auditOrder, audits).stream()
                .filter(audit -> query.targetType() == null || audit.targetType() == query.targetType())
                .filter(audit -> query.targetCode() == null || audit.targetCode().equals(query.targetCode()))
                .filter(audit -> query.createdFrom() == null || !audit.createdAt().isBefore(query.createdFrom()))
                .filter(audit -> query.createdTo() == null || !audit.createdAt().isAfter(query.createdTo()))
                .sorted(Comparator.comparing(GovernanceActionAuditRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public GovernanceActionAuditRecord saveAudit(GovernanceActionAuditRecord auditRecord) {
        audits.put(auditRecord.auditId(), auditRecord);
        updateRecency(auditOrder, auditRecord.auditId());
        return auditRecord;
    }

    private <T> List<T> orderedValues(ConcurrentLinkedDeque<String> order, Map<String, T> store) {
        return order.stream()
                .map(store::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void updateRecency(ConcurrentLinkedDeque<String> order, String id) {
        order.remove(id);
        order.addFirst(id);
    }
}
