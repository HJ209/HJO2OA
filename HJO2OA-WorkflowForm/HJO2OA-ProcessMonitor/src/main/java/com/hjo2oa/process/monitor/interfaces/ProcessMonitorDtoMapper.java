package com.hjo2oa.process.monitor.interfaces;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
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
}
