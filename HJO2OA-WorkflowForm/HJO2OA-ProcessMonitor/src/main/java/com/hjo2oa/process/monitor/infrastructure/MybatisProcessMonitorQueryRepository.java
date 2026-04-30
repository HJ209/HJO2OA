package com.hjo2oa.process.monitor.infrastructure;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.ExceptionProcessInstanceView;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.MonitoredProcessInstanceView;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.NodeTrailView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessInterventionCommand;
import com.hjo2oa.process.monitor.domain.ProcessInterventionView;
import com.hjo2oa.process.monitor.domain.ProcessMonitorQueryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisProcessMonitorQueryRepository implements ProcessMonitorQueryRepository {

    private final ProcessMonitorQueryMapper mapper;

    public MybatisProcessMonitorQueryRepository(ProcessMonitorQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter) {
        return mapper.analyzeProcessDurations(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter) {
        return mapper.findStalledNodes(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter) {
        return mapper.rankApprovalCongestion(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter) {
        return mapper.findOverdueTasks(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<MonitoredProcessInstanceView> findInstances(MonitorQueryFilter filter, String status) {
        return mapper.findInstances(filter, status).stream().map(this::toView).toList();
    }

    @Override
    public List<ExceptionProcessInstanceView> findExceptionInstances(MonitorQueryFilter filter) {
        return mapper.findExceptionInstances(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<NodeTrailView> findNodeTrail(UUID tenantId, UUID instanceId) {
        return mapper.findNodeTrail(tenantId, instanceId).stream().map(this::toView).toList();
    }

    @Override
    public List<ProcessInterventionView> findInterventions(UUID tenantId, UUID instanceId) {
        return mapper.findInterventions(tenantId, instanceId).stream().map(this::toView).toList();
    }

    @Override
    public ProcessInterventionView recordIntervention(ProcessInterventionCommand command) {
        UUID interventionId = UUID.randomUUID();
        mapper.insertIntervention(
                interventionId,
                command.tenantId(),
                command.instanceId(),
                command.taskId(),
                command.actionType(),
                command.operatorId(),
                command.targetAssigneeId(),
                command.reason()
        );
        return toView(mapper.findInterventionById(interventionId));
    }

    @Override
    public void suspendInstance(UUID tenantId, UUID instanceId) {
        mapper.suspendInstance(tenantId, instanceId);
    }

    @Override
    public void resumeInstance(UUID tenantId, UUID instanceId) {
        mapper.resumeInstance(tenantId, instanceId);
    }

    @Override
    public void terminateInstance(UUID tenantId, UUID instanceId) {
        mapper.terminateInstance(tenantId, instanceId);
        mapper.terminateOpenTasks(tenantId, instanceId);
    }

    @Override
    public void reassignTask(UUID tenantId, UUID taskId, UUID targetAssigneeId) {
        mapper.reassignTask(tenantId, taskId, targetAssigneeId);
    }

    private ProcessDurationAnalysisView toView(ProcessDurationAnalysisRow row) {
        return new ProcessDurationAnalysisView(
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                value(row.getInstanceCount()),
                value(row.getCompletedCount()),
                value(row.getRunningCount()),
                value(row.getAverageDurationMinutes()),
                value(row.getMaxDurationMinutes())
        );
    }

    private NodeStagnationAnalysisView toView(NodeStagnationAnalysisRow row) {
        return new NodeStagnationAnalysisView(
                row.getTaskId(),
                row.getInstanceId(),
                row.getInstanceTitle(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                row.getNodeId(),
                row.getNodeName(),
                row.getAssigneeId(),
                row.getTaskStatus(),
                row.getTaskCreatedAt(),
                row.getDueTime(),
                value(row.getStalledMinutes())
        );
    }

    private ApprovalCongestionAnalysisView toView(ApprovalCongestionAnalysisRow row) {
        return new ApprovalCongestionAnalysisView(
                row.getAssigneeId(),
                value(row.getPendingCount()),
                value(row.getOverdueCount()),
                row.getOldestPendingAt(),
                row.getNearestDueTime()
        );
    }

    private OverdueTaskObservationView toView(OverdueTaskObservationRow row) {
        return new OverdueTaskObservationView(
                row.getTaskId(),
                row.getInstanceId(),
                row.getInstanceTitle(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                row.getNodeId(),
                row.getNodeName(),
                row.getAssigneeId(),
                row.getDueTime(),
                value(row.getOverdueMinutes())
        );
    }

    private MonitoredProcessInstanceView toView(MonitoredProcessInstanceRow row) {
        return new MonitoredProcessInstanceView(
                row.getInstanceId(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getTitle(),
                row.getCategory(),
                row.getInitiatorId(),
                row.getStatus(),
                row.getStartTime(),
                row.getEndTime(),
                row.getUpdatedAt()
        );
    }

    private ExceptionProcessInstanceView toView(ExceptionProcessInstanceRow row) {
        return new ExceptionProcessInstanceView(
                row.getInstanceId(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getTitle(),
                row.getCategory(),
                row.getStatus(),
                row.getExceptionType(),
                value(row.getExceptionMinutes()),
                row.getDetectedAt()
        );
    }

    private NodeTrailView toView(NodeTrailRow row) {
        return new NodeTrailView(
                row.getTaskId(),
                row.getInstanceId(),
                row.getNodeId(),
                row.getNodeName(),
                row.getNodeType(),
                row.getAssigneeId(),
                row.getTaskStatus(),
                row.getCreatedAt(),
                row.getClaimTime(),
                row.getCompletedTime(),
                row.getDueTime(),
                row.getLastActionCode(),
                row.getLastActionName(),
                row.getLastOperatorId(),
                row.getLastActionAt()
        );
    }

    private ProcessInterventionView toView(ProcessInterventionRow row) {
        return new ProcessInterventionView(
                row.getInterventionId(),
                row.getInstanceId(),
                row.getTaskId(),
                row.getActionType(),
                row.getOperatorId(),
                row.getTargetAssigneeId(),
                row.getReason(),
                row.getCreatedAt()
        );
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}
