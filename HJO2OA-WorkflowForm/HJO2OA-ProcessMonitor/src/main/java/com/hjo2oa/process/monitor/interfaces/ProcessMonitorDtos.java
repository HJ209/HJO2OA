package com.hjo2oa.process.monitor.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProcessMonitorDtos {

    private ProcessMonitorDtos() {
    }

    public record ProcessDurationResponse(
            UUID definitionId,
            String definitionCode,
            String category,
            long instanceCount,
            long completedCount,
            long runningCount,
            long averageDurationMinutes,
            long maxDurationMinutes
    ) {
    }

    public record StalledNodeResponse(
            UUID taskId,
            UUID instanceId,
            String instanceTitle,
            UUID definitionId,
            String definitionCode,
            String category,
            String nodeId,
            String nodeName,
            UUID assigneeId,
            String taskStatus,
            Instant taskCreatedAt,
            Instant dueTime,
            long stalledMinutes
    ) {
    }

    public record ApprovalCongestionResponse(
            UUID assigneeId,
            long pendingCount,
            long overdueCount,
            Instant oldestPendingAt,
            Instant nearestDueTime
    ) {
    }

    public record OverdueTaskResponse(
            UUID taskId,
            UUID instanceId,
            String instanceTitle,
            UUID definitionId,
            String definitionCode,
            String category,
            String nodeId,
            String nodeName,
            UUID assigneeId,
            Instant dueTime,
            long overdueMinutes
    ) {
    }

    public record OverviewResponse(
            List<ProcessDurationResponse> processDurations,
            List<StalledNodeResponse> stalledNodes,
            List<ApprovalCongestionResponse> approvalCongestion,
            List<OverdueTaskResponse> overdueTasks
    ) {
    }
}
