package com.hjo2oa.org.data.permission.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record FieldPermissionDecisionView(
        String businessObject,
        String usageScenario,
        Map<String, Map<FieldPermissionAction, PermissionEffect>> fieldEffects,
        Set<String> hiddenFields,
        Set<String> desensitizedFields,
        List<FieldPermissionView> matchedPolicies
) {
}
