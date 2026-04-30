package com.hjo2oa.org.data.permission.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DataPermission(
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

    public DataPermission {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        businessObject = requireText(businessObject, "businessObject");
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        conditionExpr = normalizeNullable(conditionExpr);
        Objects.requireNonNull(effect, "effect must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if ((scopeType == DataScopeType.CONDITION || scopeType == DataScopeType.CUSTOM) && conditionExpr == null) {
            throw new IllegalArgumentException("conditionExpr must be provided for CONDITION or CUSTOM scope");
        }
        if (scopeType != DataScopeType.CONDITION && scopeType != DataScopeType.CUSTOM && conditionExpr != null) {
            throw new IllegalArgumentException("conditionExpr is only supported for CONDITION or CUSTOM scope");
        }
    }

    public static DataPermission create(
            UUID id,
            PermissionSubjectType subjectType,
            UUID subjectId,
            String businessObject,
            DataScopeType scopeType,
            String conditionExpr,
            PermissionEffect effect,
            int priority,
            UUID tenantId,
            Instant now
    ) {
        return new DataPermission(
                id,
                subjectType,
                subjectId,
                businessObject,
                scopeType,
                conditionExpr,
                effect == null ? PermissionEffect.ALLOW : effect,
                priority,
                tenantId,
                now,
                now
        );
    }

    public DataPermission update(
            DataScopeType nextScopeType,
            String nextConditionExpr,
            PermissionEffect nextEffect,
            int nextPriority,
            Instant now
    ) {
        return new DataPermission(
                id,
                subjectType,
                subjectId,
                businessObject,
                nextScopeType,
                nextConditionExpr,
                nextEffect,
                nextPriority,
                tenantId,
                createdAt,
                now
        );
    }

    public DataPermissionView toView() {
        return new DataPermissionView(
                id,
                subjectType,
                subjectId,
                businessObject,
                scopeType,
                conditionExpr,
                effect,
                priority,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
