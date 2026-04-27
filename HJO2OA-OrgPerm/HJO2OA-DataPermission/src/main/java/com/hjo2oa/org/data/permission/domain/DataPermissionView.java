package com.hjo2oa.org.data.permission.domain;

import java.time.Instant;
import java.util.UUID;

public record DataPermissionView(
        UUID id,
        PermissionSubjectType subjectType,
        UUID subjectId,
        String businessObject,
        DataScopeType scopeType,
        String conditionExpr,
        PermissionEffect effect,
        int priority,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
