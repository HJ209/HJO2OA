package com.hjo2oa.org.data.permission.domain;

import java.util.List;
import java.util.Map;

public record FieldPermissionDecisionView(
        String businessObject,
        String usageScenario,
        Map<String, Map<FieldPermissionAction, PermissionEffect>> fieldEffects,
        List<FieldPermissionView> matchedPolicies
) {
}
