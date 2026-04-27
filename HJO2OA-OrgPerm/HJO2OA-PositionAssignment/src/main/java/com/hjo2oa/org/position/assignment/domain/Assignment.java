package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Assignment(
        UUID id,
        UUID personId,
        UUID positionId,
        AssignmentType type,
        LocalDate startDate,
        LocalDate endDate,
        AssignmentStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Assignment {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    public static Assignment create(
            UUID id,
            UUID personId,
            UUID positionId,
            AssignmentType type,
            LocalDate startDate,
            LocalDate endDate,
            UUID tenantId,
            Instant now
    ) {
        return new Assignment(
                id,
                personId,
                positionId,
                type,
                startDate,
                endDate,
                AssignmentStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Assignment changeType(AssignmentType type, Instant now) {
        return new Assignment(
                id,
                personId,
                positionId,
                type,
                startDate,
                endDate,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Assignment deactivate(LocalDate endDate, Instant now) {
        return new Assignment(
                id,
                personId,
                positionId,
                type,
                startDate,
                endDate,
                AssignmentStatus.INACTIVE,
                tenantId,
                createdAt,
                now
        );
    }

    public AssignmentView toView() {
        return new AssignmentView(
                id,
                personId,
                positionId,
                type,
                startDate,
                endDate,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }
}
