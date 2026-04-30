package com.hjo2oa.org.data.permission.interfaces;

import com.hjo2oa.org.data.permission.application.DataPermissionCommands;
import com.hjo2oa.org.data.permission.domain.DataPermissionIdentityContext;
import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import com.hjo2oa.org.data.permission.domain.SubjectReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class DataPermissionDtos {

    private DataPermissionDtos() {
    }

    public record SaveRowPolicyRequest(
            @NotNull PermissionSubjectType subjectType,
            @NotNull UUID subjectId,
            @NotBlank @Size(max = 128) String businessObject,
            @NotNull DataScopeType scopeType,
            @Size(max = 1024) String conditionExpr,
            PermissionEffect effect,
            int priority,
            UUID tenantId
    ) {

        public DataPermissionCommands.SaveRowPolicyCommand toCommand() {
            return new DataPermissionCommands.SaveRowPolicyCommand(
                    subjectType,
                    subjectId,
                    businessObject,
                    scopeType,
                    conditionExpr,
                    effect,
                    priority,
                    tenantId
            );
        }
    }

    public record SaveFieldPolicyRequest(
            @NotNull PermissionSubjectType subjectType,
            @NotNull UUID subjectId,
            @NotBlank @Size(max = 128) String businessObject,
            @NotBlank @Size(max = 64) String usageScenario,
            @NotBlank @Size(max = 128) String fieldCode,
            @NotNull FieldPermissionAction action,
            PermissionEffect effect,
            UUID tenantId
    ) {

        public DataPermissionCommands.SaveFieldPolicyCommand toCommand() {
            return new DataPermissionCommands.SaveFieldPolicyCommand(
                    subjectType,
                    subjectId,
                    businessObject,
                    usageScenario,
                    fieldCode,
                    action,
                    effect,
                    tenantId
            );
        }
    }

    public record SubjectRequest(
            @NotNull PermissionSubjectType subjectType,
            @NotNull UUID subjectId
    ) {

        public SubjectReference toReference() {
            return new SubjectReference(subjectType, subjectId);
        }
    }

    public record IdentityContextRequest(
            @NotNull UUID tenantId,
            @NotNull UUID personId,
            UUID organizationId,
            UUID departmentId,
            @NotNull UUID positionId,
            List<@NotNull UUID> roleIds
    ) {

        public DataPermissionIdentityContext toContext() {
            return new DataPermissionIdentityContext(
                    tenantId,
                    personId,
                    organizationId,
                    departmentId,
                    positionId,
                    roleIds == null ? List.of() : roleIds
            );
        }
    }

    public record RowDecisionRequest(
            @NotBlank @Size(max = 128) String businessObject,
            UUID tenantId,
            IdentityContextRequest identityContext,
            List<@Valid SubjectRequest> subjects
    ) {

        public DataPermissionCommands.RowDecisionQuery toQuery() {
            List<SubjectReference> resolvedSubjects = resolveSubjects(identityContext, subjects);
            return new DataPermissionCommands.RowDecisionQuery(
                    businessObject,
                    identityContext == null ? tenantId : identityContext.tenantId(),
                    resolvedSubjects
            );
        }
    }

    public record FieldDecisionRequest(
            @NotBlank @Size(max = 128) String businessObject,
            @NotBlank @Size(max = 64) String usageScenario,
            UUID tenantId,
            IdentityContextRequest identityContext,
            List<@NotBlank @Size(max = 128) String> fieldCodes,
            List<@Valid SubjectRequest> subjects
    ) {

        public DataPermissionCommands.FieldDecisionQuery toQuery() {
            List<SubjectReference> resolvedSubjects = resolveSubjects(identityContext, subjects);
            return new DataPermissionCommands.FieldDecisionQuery(
                    businessObject,
                    usageScenario,
                    identityContext == null ? tenantId : identityContext.tenantId(),
                    fieldCodes == null ? List.of() : fieldCodes,
                    resolvedSubjects
            );
        }
    }

    public record RowPolicyResponse(
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

    public record FieldPolicyResponse(
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

    public record RowDecisionResponse(
            String businessObject,
            boolean allowed,
            DataScopeType scopeType,
            String conditionExpr,
            String sqlCondition,
            PermissionEffect effect,
            List<RowPolicyResponse> matchedPolicies
    ) {
    }

    public record FieldDecisionResponse(
            String businessObject,
            String usageScenario,
            Object fieldEffects,
            Set<String> hiddenFields,
            Set<String> desensitizedFields,
            List<FieldPolicyResponse> matchedPolicies
    ) {
    }

    public record FieldMaskRequest(
            @NotBlank @Size(max = 128) String businessObject,
            @NotBlank @Size(max = 64) String usageScenario,
            @NotNull IdentityContextRequest identityContext,
            @NotNull Map<String, Object> row
    ) {
    }

    public record FieldMaskResponse(
            Map<String, Object> row,
            FieldDecisionResponse decision
    ) {
    }

    private static List<SubjectReference> resolveSubjects(
            IdentityContextRequest identityContext,
            List<SubjectRequest> subjects
    ) {
        if (identityContext != null) {
            return identityContext.toContext().toSubjects();
        }
        return Objects.requireNonNullElse(subjects, List.<SubjectRequest>of()).stream()
                .map(SubjectRequest::toReference)
                .toList();
    }
}
