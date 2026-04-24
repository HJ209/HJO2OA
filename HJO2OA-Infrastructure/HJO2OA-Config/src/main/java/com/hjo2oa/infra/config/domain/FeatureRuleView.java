package com.hjo2oa.infra.config.domain;

import java.util.UUID;

public record FeatureRuleView(
        UUID id,
        UUID configEntryId,
        FeatureRuleType ruleType,
        String ruleValue,
        int sortOrder,
        boolean active
) {
}
