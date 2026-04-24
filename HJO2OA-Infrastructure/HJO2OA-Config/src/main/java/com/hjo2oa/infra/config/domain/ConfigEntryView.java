package com.hjo2oa.infra.config.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConfigEntryView(
        UUID id,
        String configKey,
        String name,
        ConfigType configType,
        String defaultValue,
        String validationRule,
        boolean mutableAtRuntime,
        ConfigStatus status,
        boolean tenantAware,
        Instant createdAt,
        Instant updatedAt,
        List<ConfigOverrideView> overrides,
        List<FeatureRuleView> featureRules
) {

    public ConfigEntryView {
        overrides = overrides == null ? List.of() : List.copyOf(overrides);
        featureRules = featureRules == null ? List.of() : List.copyOf(featureRules);
    }
}
