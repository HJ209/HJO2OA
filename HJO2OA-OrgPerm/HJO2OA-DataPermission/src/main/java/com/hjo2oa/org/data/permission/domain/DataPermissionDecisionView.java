package com.hjo2oa.org.data.permission.domain;

import java.util.List;

public record DataPermissionDecisionView(
        String businessObject,
        boolean allowed,
        DataScopeType scopeType,
        String conditionExpr,
        PermissionEffect effect,
        List<DataPermissionView> matchedPolicies
) {
}
