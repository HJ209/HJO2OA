package com.hjo2oa.org.data.permission.domain;

import java.util.UUID;

public record FieldPermissionQuery(
        PermissionSubjectType subjectType,
        UUID subjectId,
        String businessObject,
        String usageScenario,
        String fieldCode,
        FieldPermissionAction action,
        PermissionEffect effect,
        UUID tenantId
) {
}
