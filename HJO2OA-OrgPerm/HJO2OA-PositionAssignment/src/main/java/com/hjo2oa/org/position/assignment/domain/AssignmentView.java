package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssignmentView(
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
}
