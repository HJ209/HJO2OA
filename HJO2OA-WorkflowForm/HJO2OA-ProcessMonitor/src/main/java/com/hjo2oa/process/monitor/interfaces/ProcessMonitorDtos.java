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

    public record MonitoredInstanceResponse(
            UUID instanceId,
            UUID definitionId,
            String definitionCode,
            String title,
            String category,
            UUID initiatorId,
            String status,
            Instant startTime,
            Instant endTime,
            Instant updatedAt
    ) {
    }

    public record ExceptionInstanceResponse(
            UUID instanceId,
            UUID definitionId,
            String definitionCode,
            String title,
            String category,
            String status,
            String exceptionType,
            long exceptionMinutes,
            Instant detectedAt
    ) {
    }

    public record NodeTrailResponse(
            UUID taskId,
            UUID instanceId,
            String nodeId,
            String nodeName,
            String nodeType,
            UUID assigneeId,
            String taskStatus,
            Instant createdAt,
            Instant claimTime,
            Instant completedTime,
            Instant dueTime,
            String lastActionCode,
            String lastActionName,
            UUID lastOperatorId,
            Instant lastActionAt
    ) {
    }

    public record InterventionResponse(
            UUID interventionId,
            UUID instanceId,
            UUID taskId,
            String actionType,
            UUID operatorId,
            UUID targetAssigneeId,
            String reason,
            Instant createdAt
    ) {
    }

    public record InterventionRequest(
            UUID tenantId,
            UUID taskId,
            String actionType,
            UUID operatorId,
            UUID targetAssigneeId,
            String reason
    ) {
    }
}
