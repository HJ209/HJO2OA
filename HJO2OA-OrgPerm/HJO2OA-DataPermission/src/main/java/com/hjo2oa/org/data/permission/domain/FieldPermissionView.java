package com.hjo2oa.org.data.permission.domain;

import java.time.Instant;
import java.util.UUID;

public record FieldPermissionView(
        UUID id,
        PermissionSubjectType subjectType,
        UUID subjectId,
        String businessObject,
        String usageScenario,
        String fieldCode,
        FieldPermissionAction action,
        PermissionEffect effect,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
