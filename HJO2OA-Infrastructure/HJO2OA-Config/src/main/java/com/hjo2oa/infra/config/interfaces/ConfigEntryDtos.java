package com.hjo2oa.infra.config.interfaces;

import com.hjo2oa.infra.config.application.ConfigEntryCommands;
import com.hjo2oa.infra.config.domain.ConfigStatus;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.FeatureRuleType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import com.hjo2oa.infra.config.domain.ResolvedValueSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ConfigEntryDtos {

    private ConfigEntryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 128) String configKey,
            @NotBlank @Size(max = 128) String name,
            @NotNull ConfigType configType,
            @NotNull String defaultValue,
            Boolean mutableAtRuntime,
            Boolean tenantAware,
            String validationRule
    ) {

        public ConfigEntryCommands.CreateEntryCommand toCommand() {
            return new ConfigEntryCommands.CreateEntryCommand(
                    configKey,
                    name,
                    configType,
                    defaultValue,
                    mutableAtRuntime == null || mutableAtRuntime,
                    Boolean.TRUE.equals(tenantAware),
                    validationRule
            );
        }
    }

    public record UpdateDefaultRequest(
            @NotNull String defaultValue
    ) {
    }

    public record AddOverrideRequest(
            @NotNull OverrideScopeType scopeType,
            @NotNull UUID scopeId,
            @NotNull String overrideValue
    ) {

        public ConfigEntryCommands.AddOverrideCommand toCommand(UUID entryId) {
            return new ConfigEntryCommands.AddOverrideCommand(entryId, scopeType, scopeId, overrideValue);
        }
    }

    public record AddFeatureRuleRequest(
            @NotNull FeatureRuleType ruleType,
            @NotNull String ruleValue,
            @PositiveOrZero Integer sortOrder
    ) {

        public ConfigEntryCommands.AddFeatureRuleCommand toCommand(UUID entryId) {
            return new ConfigEntryCommands.AddFeatureRuleCommand(entryId, ruleType, ruleValue, sortOrder);
        }
    }

    public record UpdateFeatureRuleRequest(
            FeatureRuleType ruleType,
            String ruleValue,
            @PositiveOrZero Integer sortOrder,
            Boolean active
    ) {

        public ConfigEntryCommands.UpdateFeatureRuleCommand toCommand(UUID ruleId) {
            return new ConfigEntryCommands.UpdateFeatureRuleCommand(ruleId, ruleType, ruleValue, sortOrder, active);
        }
    }

    public record ResolveValueRequest(
            @NotBlank String key,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {

        public ConfigEntryCommands.ResolveValueQuery toQuery() {
            return new ConfigEntryCommands.ResolveValueQuery(key, tenantId, orgId, roleId, userId);
        }
    }

    public record ConfigEntryResponse(
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
            List<ConfigOverrideResponse> overrides,
            List<FeatureRuleResponse> featureRules
    ) {
    }

    public record ConfigOverrideResponse(
            UUID id,
            UUID configEntryId,
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue,
            boolean active
    ) {
    }

    public record FeatureRuleResponse(
            UUID id,
            UUID configEntryId,
            FeatureRuleType ruleType,
            String ruleValue,
            int sortOrder,
            boolean active
    ) {
    }

    public record ResolvedConfigValueResponse(
            UUID entryId,
            String configKey,
            ConfigType configType,
            ConfigStatus status,
            String resolvedValue,
            ResolvedValueSourceType sourceType,
            UUID overrideId,
            UUID featureRuleId,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId,
            List<String> trace
    ) {
    }
}
