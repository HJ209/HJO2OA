package com.hjo2oa.org.data.permission.application;

import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import com.hjo2oa.org.data.permission.domain.SubjectReference;
import java.util.List;
import java.util.UUID;

public final class DataPermissionCommands {

    private DataPermissionCommands() {
    }

    public record SaveRowPolicyCommand(
            PermissionSubjectType subjectType,
            UUID subjectId,
            String businessObject,
            DataScopeType scopeType,
            String conditionExpr,
            PermissionEffect effect,
            int priority,
            UUID tenantId
    ) {
    }

    public record SaveFieldPolicyCommand(
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

    public record RowDecisionQuery(
            String businessObject,
            UUID tenantId,
            List<SubjectReference> subjects
    ) {
    }

    public record FieldDecisionQuery(
            String businessObject,
            String usageScenario,
            UUID tenantId,
            List<String> fieldCodes,
            List<SubjectReference> subjects
    ) {
    }
}
