package com.hjo2oa.org.data.permission.domain;

import java.util.UUID;

public record DataPermissionQuery(
        PermissionSubjectType subjectType,
        UUID subjectId,
        String businessObject,
        DataScopeType scopeType,
        PermissionEffect effect,
        UUID tenantId
) {
}
