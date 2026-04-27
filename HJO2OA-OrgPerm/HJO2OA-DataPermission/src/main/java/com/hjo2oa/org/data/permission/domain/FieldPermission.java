package com.hjo2oa.org.data.permission.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FieldPermission(
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

    public FieldPermission {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
        businessObject = DataPermission.requireText(businessObject, "businessObject");
        usageScenario = DataPermission.requireText(usageScenario, "usageScenario");
        fieldCode = DataPermission.requireText(fieldCode, "fieldCode");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(effect, "effect must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static FieldPermission create(
            UUID id,
            PermissionSubjectType subjectType,
            UUID subjectId,
            String businessObject,
            String usageScenario,
            String fieldCode,
            FieldPermissionAction action,
            PermissionEffect effect,
            UUID tenantId,
            Instant now
    ) {
        return new FieldPermission(
                id,
                subjectType,
                subjectId,
                businessObject,
                usageScenario,
                fieldCode,
                action,
                effect == null ? PermissionEffect.ALLOW : effect,
                tenantId,
                now,
                now
        );
    }

    public FieldPermission update(FieldPermissionAction nextAction, PermissionEffect nextEffect, Instant now) {
        return new FieldPermission(
                id,
                subjectType,
                subjectId,
                businessObject,
                usageScenario,
                fieldCode,
                nextAction,
                nextEffect,
                tenantId,
                createdAt,
                now
        );
    }

    public FieldPermissionView toView() {
        return new FieldPermissionView(
                id,
                subjectType,
                subjectId,
                businessObject,
                usageScenario,
                fieldCode,
                action,
                effect,
                tenantId,
                createdAt,
                updatedAt
        );
    }
}
