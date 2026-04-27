package com.hjo2oa.wf.process.instance.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessInstance(
        UUID id,
        UUID definitionId,
        int definitionVersion,
        String definitionCode,
        String title,
        String category,
        UUID initiatorId,
        UUID initiatorOrgId,
        UUID initiatorDeptId,
        UUID initiatorPositionId,
        UUID formMetadataId,
        UUID formDataId,
        List<String> currentNodes,
        ProcessInstanceStatus status,
        Instant startTime,
        Instant endTime,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public ProcessInstance {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(definitionId, "definitionId must not be null");
        definitionCode = requireText(definitionCode, "definitionCode");
        title = requireText(title, "title");
        Objects.requireNonNull(initiatorId, "initiatorId must not be null");
        Objects.requireNonNull(initiatorOrgId, "initiatorOrgId must not be null");
        Objects.requireNonNull(initiatorPositionId, "initiatorPositionId must not be null");
        Objects.requireNonNull(formMetadataId, "formMetadataId must not be null");
        Objects.requireNonNull(formDataId, "formDataId must not be null");
        currentNodes = normalizeNodes(currentNodes);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ProcessInstance start(
            UUID definitionId,
            int definitionVersion,
            String definitionCode,
            String title,
            String category,
            UUID initiatorId,
            UUID initiatorOrgId,
            UUID initiatorDeptId,
            UUID initiatorPositionId,
            UUID formMetadataId,
            UUID formDataId,
            List<String> currentNodes,
            UUID tenantId,
            Instant now
    ) {
        return new ProcessInstance(
                UUID.randomUUID(),
                definitionId,
                definitionVersion,
                definitionCode,
                title,
                category,
                initiatorId,
                initiatorOrgId,
                initiatorDeptId,
                initiatorPositionId,
                formMetadataId,
                formDataId,
                currentNodes,
                ProcessInstanceStatus.RUNNING,
                now,
                null,
                tenantId,
                now,
                now
        );
    }

    public ProcessInstance moveTo(List<String> nodeIds, Instant now) {
        requireRunning();
        return copy(nodeIds, status, endTime, now);
    }

    public ProcessInstance complete(Instant now) {
        requireRunning();
        return copy(List.of(), ProcessInstanceStatus.COMPLETED, now, now);
    }

    public ProcessInstance terminate(String reason, Instant now) {
        if (status == ProcessInstanceStatus.COMPLETED || status == ProcessInstanceStatus.TERMINATED) {
            return this;
        }
        return copy(List.of(), ProcessInstanceStatus.TERMINATED, now, now);
    }

    public ProcessInstance suspend(Instant now) {
        requireRunning();
        return copy(currentNodes, ProcessInstanceStatus.SUSPENDED, endTime, now);
    }

    public ProcessInstance resume(Instant now) {
        if (status != ProcessInstanceStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended process instance can be resumed");
        }
        return copy(currentNodes, ProcessInstanceStatus.RUNNING, endTime, now);
    }

    public ProcessInstanceViews.ProcessInstanceView toView() {
        return new ProcessInstanceViews.ProcessInstanceView(
                id,
                definitionId,
                definitionVersion,
                definitionCode,
                title,
                category,
                initiatorId,
                initiatorOrgId,
                initiatorDeptId,
                initiatorPositionId,
                formMetadataId,
                formDataId,
                currentNodes,
                status,
                startTime,
                endTime,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private ProcessInstance copy(
            List<String> nodeIds,
            ProcessInstanceStatus nextStatus,
            Instant nextEndTime,
            Instant now
    ) {
        return new ProcessInstance(
                id,
                definitionId,
                definitionVersion,
                definitionCode,
                title,
                category,
                initiatorId,
                initiatorOrgId,
                initiatorDeptId,
                initiatorPositionId,
                formMetadataId,
                formDataId,
                nodeIds,
                nextStatus,
                startTime,
                nextEndTime,
                tenantId,
                createdAt,
                now
        );
    }

    private void requireRunning() {
        if (status != ProcessInstanceStatus.RUNNING) {
            throw new IllegalStateException("Process instance is not running: " + status);
        }
    }

    private static List<String> normalizeNodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
