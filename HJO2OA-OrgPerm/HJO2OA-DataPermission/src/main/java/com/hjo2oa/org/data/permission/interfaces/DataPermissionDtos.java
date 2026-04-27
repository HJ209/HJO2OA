package com.hjo2oa.org.data.permission.interfaces;

import com.hjo2oa.org.data.permission.application.DataPermissionCommands;
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

    public record RowDecisionRequest(
            @NotBlank @Size(max = 128) String businessObject,
            UUID tenantId,
            @NotEmpty List<@Valid SubjectRequest> subjects
    ) {

        public DataPermissionCommands.RowDecisionQuery toQuery() {
            return new DataPermissionCommands.RowDecisionQuery(
                    businessObject,
                    tenantId,
                    subjects.stream().map(SubjectRequest::toReference).toList()
            );
        }
    }

    public record FieldDecisionRequest(
            @NotBlank @Size(max = 128) String businessObject,
            @NotBlank @Size(max = 64) String usageScenario,
            UUID tenantId,
            List<@NotBlank @Size(max = 128) String> fieldCodes,
            @NotEmpty List<@Valid SubjectRequest> subjects
    ) {

        public DataPermissionCommands.FieldDecisionQuery toQuery() {
            return new DataPermissionCommands.FieldDecisionQuery(
                    businessObject,
                    usageScenario,
                    tenantId,
                    fieldCodes == null ? List.of() : fieldCodes,
                    subjects.stream().map(SubjectRequest::toReference).toList()
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
            PermissionEffect effect,
            List<RowPolicyResponse> matchedPolicies
    ) {
    }

    public record FieldDecisionResponse(
            String businessObject,
            String usageScenario,
            Object fieldEffects,
            List<FieldPolicyResponse> matchedPolicies
    ) {
    }
}
