package com.hjo2oa.org.data.permission.interfaces;

import com.hjo2oa.org.data.permission.domain.DataPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.DataPermissionView;
import com.hjo2oa.org.data.permission.domain.FieldPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.FieldPermissionView;
import org.springframework.stereotype.Component;

@Component
public class DataPermissionDtoMapper {

    public DataPermissionDtos.RowPolicyResponse toRowPolicyResponse(DataPermissionView view) {
        return new DataPermissionDtos.RowPolicyResponse(
                view.id(),
                view.subjectType(),
                view.subjectId(),
                view.businessObject(),
                view.scopeType(),
                view.conditionExpr(),
                view.effect(),
                view.priority(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public DataPermissionDtos.FieldPolicyResponse toFieldPolicyResponse(FieldPermissionView view) {
        return new DataPermissionDtos.FieldPolicyResponse(
                view.id(),
                view.subjectType(),
                view.subjectId(),
                view.businessObject(),
                view.usageScenario(),
                view.fieldCode(),
                view.action(),
                view.effect(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public DataPermissionDtos.RowDecisionResponse toRowDecisionResponse(DataPermissionDecisionView view) {
        return new DataPermissionDtos.RowDecisionResponse(
                view.businessObject(),
                view.allowed(),
                view.scopeType(),
                view.conditionExpr(),
                view.sqlCondition(),
                view.effect(),
                view.matchedPolicies().stream().map(this::toRowPolicyResponse).toList()
        );
    }

    public DataPermissionDtos.FieldDecisionResponse toFieldDecisionResponse(FieldPermissionDecisionView view) {
        return new DataPermissionDtos.FieldDecisionResponse(
                view.businessObject(),
                view.usageScenario(),
                view.fieldEffects(),
                view.hiddenFields(),
                view.desensitizedFields(),
                view.matchedPolicies().stream().map(this::toFieldPolicyResponse).toList()
        );
    }
}
