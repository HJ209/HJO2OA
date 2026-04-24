package com.hjo2oa.infra.config.domain;

import java.util.Objects;
import java.util.UUID;

public record FeatureRule(
        UUID id,
        UUID configEntryId,
        FeatureRuleType ruleType,
        String ruleValue,
        int sortOrder,
        boolean active
) {

    public FeatureRule {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(configEntryId, "configEntryId must not be null");
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        Objects.requireNonNull(ruleValue, "ruleValue must not be null");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }

    public static FeatureRule active(
            UUID configEntryId,
            FeatureRuleType ruleType,
            String ruleValue,
            int sortOrder
    ) {
        return new FeatureRule(UUID.randomUUID(), configEntryId, ruleType, ruleValue, sortOrder, true);
    }

    public FeatureRule deactivate() {
        if (!active) {
            return this;
        }
        return new FeatureRule(id, configEntryId, ruleType, ruleValue, sortOrder, false);
    }

    public FeatureRuleView toView() {
        return new FeatureRuleView(id, configEntryId, ruleType, ruleValue, sortOrder, active);
    }
}
