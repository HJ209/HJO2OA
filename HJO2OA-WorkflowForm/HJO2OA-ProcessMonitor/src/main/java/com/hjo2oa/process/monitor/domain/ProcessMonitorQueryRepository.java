package com.hjo2oa.process.monitor.domain;

import java.util.List;
import java.util.UUID;

public interface ProcessMonitorQueryRepository {

    List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter);

    List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter);

    List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter);

    List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter);

    List<MonitoredProcessInstanceView> findInstances(MonitorQueryFilter filter, String status);

    List<ExceptionProcessInstanceView> findExceptionInstances(MonitorQueryFilter filter);

    List<NodeTrailView> findNodeTrail(UUID tenantId, UUID instanceId);

    List<ProcessInterventionView> findInterventions(UUID tenantId, UUID instanceId);

    ProcessInterventionView recordIntervention(ProcessInterventionCommand command);

    void suspendInstance(UUID tenantId, UUID instanceId);

    void resumeInstance(UUID tenantId, UUID instanceId);

    void terminateInstance(UUID tenantId, UUID instanceId);

    void reassignTask(UUID tenantId, UUID taskId, UUID targetAssigneeId);
}
