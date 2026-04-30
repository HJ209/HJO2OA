package com.hjo2oa.infra.config.application;

import com.hjo2oa.infra.config.domain.ConfigStatus;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.FeatureRuleType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import java.util.UUID;

public final class ConfigEntryCommands {

    private ConfigEntryCommands() {
    }

    public record CreateEntryCommand(
            String configKey,
            String name,
            ConfigType configType,
            String defaultValue,
            boolean mutableAtRuntime,
            boolean tenantAware,
            String validationRule
    ) {
    }

    public record UpdateDefaultCommand(
            UUID entryId,
            String defaultValue
    ) {
    }

    public record AddOverrideCommand(
            UUID entryId,
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue
    ) {
    }

    public record AddFeatureRuleCommand(
            UUID entryId,
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder
    ) {
    }

    public record UpdateFeatureRuleCommand(
            UUID ruleId,
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder,
            Boolean active
    ) {
    }

    public record ResolveValueQuery(
            String key,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
    }

    public record ListQuery(
            String configKey,
            String keyword,
            ConfigType configType,
            ConfigStatus status,
            Boolean tenantAware,
            Boolean mutableAtRuntime
    ) {
    }
}
