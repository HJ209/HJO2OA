package com.hjo2oa.process.monitor.interfaces;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.ExceptionProcessInstanceView;
import com.hjo2oa.process.monitor.domain.MonitoredProcessInstanceView;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.NodeTrailView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessInterventionView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProcessMonitorDtoMapper {

    public List<ProcessMonitorDtos.ProcessDurationResponse> toDurationResponses(
            List<ProcessDurationAnalysisView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.StalledNodeResponse> toStalledNodeResponses(
            List<NodeStagnationAnalysisView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.ApprovalCongestionResponse> toCongestionResponses(
            List<ApprovalCongestionAnalysisView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.OverdueTaskResponse> toOverdueTaskResponses(
            List<OverdueTaskObservationView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.MonitoredInstanceResponse> toInstanceResponses(
            List<MonitoredProcessInstanceView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.ExceptionInstanceResponse> toExceptionResponses(
            List<ExceptionProcessInstanceView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.NodeTrailResponse> toNodeTrailResponses(List<NodeTrailView> views) {
        return views.stream().map(this::toResponse).toList();
    }

    public List<ProcessMonitorDtos.InterventionResponse> toInterventionResponses(
            List<ProcessInterventionView> views
    ) {
        return views.stream().map(this::toResponse).toList();
    }

    public ProcessMonitorDtos.InterventionResponse toInterventionResponse(ProcessInterventionView view) {
        return toResponse(view);
    }

    private ProcessMonitorDtos.ProcessDurationResponse toResponse(ProcessDurationAnalysisView view) {
        return new ProcessMonitorDtos.ProcessDurationResponse(
                view.definitionId(),
                view.definitionCode(),
                view.category(),
                view.instanceCount(),
                view.completedCount(),
                view.runningCount(),
                view.averageDurationMinutes(),
                view.maxDurationMinutes()
        );
    }

    private ProcessMonitorDtos.StalledNodeResponse toResponse(NodeStagnationAnalysisView view) {
        return new ProcessMonitorDtos.StalledNodeResponse(
                view.taskId(),
                view.instanceId(),
                view.instanceTitle(),
                view.definitionId(),
                view.definitionCode(),
                view.category(),
                view.nodeId(),
                view.nodeName(),
                view.assigneeId(),
                view.taskStatus(),
                view.taskCreatedAt(),
                view.dueTime(),
                view.stalledMinutes()
        );
    }

    private ProcessMonitorDtos.ApprovalCongestionResponse toResponse(ApprovalCongestionAnalysisView view) {
        return new ProcessMonitorDtos.ApprovalCongestionResponse(
                view.assigneeId(),
                view.pendingCount(),
                view.overdueCount(),
                view.oldestPendingAt(),
                view.nearestDueTime()
        );
    }

    private ProcessMonitorDtos.OverdueTaskResponse toResponse(OverdueTaskObservationView view) {
        return new ProcessMonitorDtos.OverdueTaskResponse(
                view.taskId(),
                view.instanceId(),
                view.instanceTitle(),
                view.definitionId(),
                view.definitionCode(),
                view.category(),
                view.nodeId(),
                view.nodeName(),
                view.assigneeId(),
                view.dueTime(),
                view.overdueMinutes()
        );
    }

    private ProcessMonitorDtos.MonitoredInstanceResponse toResponse(MonitoredProcessInstanceView view) {
        return new ProcessMonitorDtos.MonitoredInstanceResponse(
                view.instanceId(),
                view.definitionId(),
                view.definitionCode(),
                view.title(),
                view.category(),
                view.initiatorId(),
                view.status(),
                view.startTime(),
                view.endTime(),
                view.updatedAt()
        );
    }

    private ProcessMonitorDtos.ExceptionInstanceResponse toResponse(ExceptionProcessInstanceView view) {
        return new ProcessMonitorDtos.ExceptionInstanceResponse(
                view.instanceId(),
                view.definitionId(),
                view.definitionCode(),
                view.title(),
                view.category(),
                view.status(),
                view.exceptionType(),
                view.exceptionMinutes(),
                view.detectedAt()
        );
    }

    private ProcessMonitorDtos.NodeTrailResponse toResponse(NodeTrailView view) {
        return new ProcessMonitorDtos.NodeTrailResponse(
                view.taskId(),
                view.instanceId(),
                view.nodeId(),
                view.nodeName(),
                view.nodeType(),
                view.assigneeId(),
                view.taskStatus(),
                view.createdAt(),
                view.claimTime(),
                view.completedTime(),
                view.dueTime(),
                view.lastActionCode(),
                view.lastActionName(),
                view.lastOperatorId(),
                view.lastActionAt()
        );
    }

    private ProcessMonitorDtos.InterventionResponse toResponse(ProcessInterventionView view) {
        return new ProcessMonitorDtos.InterventionResponse(
                view.interventionId(),
                view.instanceId(),
                view.taskId(),
                view.actionType(),
                view.operatorId(),
                view.targetAssigneeId(),
                view.reason(),
                view.createdAt()
        );
    }
}
