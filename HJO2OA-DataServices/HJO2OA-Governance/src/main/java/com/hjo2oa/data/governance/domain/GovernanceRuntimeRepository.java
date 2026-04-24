package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.HealthSnapshotQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.util.List;
import java.util.Optional;

public interface GovernanceRuntimeRepository {

    Optional<GovernanceRuntimeSignal> findSignal(String tenantId, GovernanceScopeType targetType, String targetCode);

    List<GovernanceRuntimeSignal> findAllSignals();

    GovernanceRuntimeSignal saveSignal(GovernanceRuntimeSignal signal);

    Optional<GovernanceHealthSnapshot> findLatestSnapshot(String governanceId, String ruleId);

    List<GovernanceHealthSnapshot> findSnapshots(HealthSnapshotQuery query);

    GovernanceHealthSnapshot saveSnapshot(GovernanceHealthSnapshot snapshot);

    Optional<GovernanceAlertRecord> findAlertById(String alertId);

    Optional<GovernanceAlertRecord> findOpenAlertByKey(String alertKey);

    List<GovernanceAlertRecord> findAlerts(AlertQuery query);

    GovernanceAlertRecord saveAlert(GovernanceAlertRecord alertRecord);

    Optional<GovernanceTraceRecord> findTraceById(String traceId);

    List<GovernanceTraceRecord> findTraces(TraceQuery query);

    GovernanceTraceRecord saveTrace(GovernanceTraceRecord traceRecord);

    Optional<GovernanceActionAuditRecord> findAuditByRequestId(String requestId);

    List<GovernanceActionAuditRecord> findAudits(AuditQuery query);

    GovernanceActionAuditRecord saveAudit(GovernanceActionAuditRecord auditRecord);
}
