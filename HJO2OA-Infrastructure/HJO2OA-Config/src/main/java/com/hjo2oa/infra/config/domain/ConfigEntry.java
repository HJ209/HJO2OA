package com.hjo2oa.infra.config.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record ConfigEntry(
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
        List<ConfigOverride> overrides,
        List<FeatureRule> featureRules
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final List<OverrideScopeType> OVERRIDE_PRECEDENCE = List.of(
            OverrideScopeType.TENANT,
            OverrideScopeType.ORGANIZATION,
            OverrideScopeType.ROLE,
            OverrideScopeType.USER
    );

    public ConfigEntry {
        Objects.requireNonNull(id, "id must not be null");
        configKey = requireText(configKey, "configKey", 128);
        name = requireText(name, "name", 128);
        Objects.requireNonNull(configType, "configType must not be null");
        defaultValue = requireConfigValue(configType, defaultValue, validationRule);
        validationRule = normalizeOptionalStructuredValue(validationRule);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        overrides = normalizeOverrides(id, overrides);
        featureRules = normalizeFeatureRules(id, featureRules);
    }

    public static ConfigEntry create(
            String configKey,
            String name,
            ConfigType configType,
            String defaultValue,
            boolean mutableAtRuntime,
            boolean tenantAware,
            String validationRule,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new ConfigEntry(
                UUID.randomUUID(),
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                ConfigStatus.ACTIVE,
                tenantAware,
                now,
                now,
                List.of(),
                List.of()
        );
    }

    public ConfigEntry disable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == ConfigStatus.DISABLED) {
            return this;
        }
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                ConfigStatus.DISABLED,
                tenantAware,
                createdAt,
                now,
                overrides,
                featureRules
        );
    }

    public ConfigEntry updateDefault(String newDefaultValue, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        String normalizedDefaultValue = requireConfigValue(configType, newDefaultValue, validationRule);
        if (Objects.equals(defaultValue, normalizedDefaultValue)) {
            return this;
        }
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                normalizedDefaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                overrides,
                featureRules
        );
    }

    public ConfigEntry addOverride(
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue,
            Instant now
    ) {
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (scopeType == OverrideScopeType.TENANT && !tenantAware) {
            throw new IllegalArgumentException("tenant scoped override is not allowed");
        }
        if (overrides.stream().anyMatch(existing -> existing.scopeType() == scopeType && existing.scopeId().equals(scopeId))) {
            throw new IllegalArgumentException("override already exists for scope");
        }
        ConfigOverride configOverride = ConfigOverride.active(
                id,
                scopeType,
                scopeId,
                requireConfigValue(configType, overrideValue, validationRule)
        );
        List<ConfigOverride> newOverrides = overrides.stream().toList();
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                appendOverride(newOverrides, configOverride),
                featureRules
        );
    }

    public ConfigEntry removeOverride(UUID overrideId, Instant now) {
        Objects.requireNonNull(overrideId, "overrideId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (overrides.stream().noneMatch(configOverride -> configOverride.id().equals(overrideId))) {
            return this;
        }
        List<ConfigOverride> newOverrides = overrides.stream()
                .filter(configOverride -> !configOverride.id().equals(overrideId))
                .toList();
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                newOverrides,
                featureRules
        );
    }

    public ConfigEntry deactivateOverride(UUID overrideId, Instant now) {
        Objects.requireNonNull(overrideId, "overrideId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (overrides.stream().noneMatch(configOverride -> configOverride.id().equals(overrideId)
                && configOverride.active())) {
            return this;
        }
        List<ConfigOverride> newOverrides = overrides.stream()
                .map(configOverride -> configOverride.id().equals(overrideId)
                        ? configOverride.deactivate()
                        : configOverride)
                .toList();
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                newOverrides,
                featureRules
        );
    }

    public ConfigEntry addFeatureRule(
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder,
            Instant now
    ) {
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (configType != ConfigType.FEATURE_FLAG) {
            throw new IllegalArgumentException("feature rules are only supported for feature flag configs");
        }
        int normalizedSortOrder = sortOrder == null ? nextSortOrder() : sortOrder;
        if (featureRules.stream().anyMatch(existing -> existing.sortOrder() == normalizedSortOrder)) {
            throw new IllegalArgumentException("feature rule sort order already exists");
        }
        FeatureRule featureRule = FeatureRule.active(
                id,
                ruleType,
                requireFeatureRuleValue(ruleType, ruleValue),
                normalizedSortOrder
        );
        List<FeatureRule> newFeatureRules = featureRules.stream().toList();
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                overrides,
                appendFeatureRule(newFeatureRules, featureRule)
        );
    }

    public ConfigEntry updateFeatureRule(
            UUID ruleId,
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder,
            Boolean active,
            Instant now
    ) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (configType != ConfigType.FEATURE_FLAG) {
            throw new IllegalArgumentException("feature rules are only supported for feature flag configs");
        }
        FeatureRule existingRule = featureRules.stream()
                .filter(rule -> rule.id().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("feature rule not found"));
        FeatureRuleType nextRuleType = ruleType == null ? existingRule.ruleType() : ruleType;
        int nextSortOrder = sortOrder == null ? existingRule.sortOrder() : sortOrder;
        if (featureRules.stream().anyMatch(rule -> !rule.id().equals(ruleId) && rule.sortOrder() == nextSortOrder)) {
            throw new IllegalArgumentException("feature rule sort order already exists");
        }
        String nextRuleValue = ruleValue == null ? existingRule.ruleValue() : ruleValue;
        FeatureRule updatedRule = new FeatureRule(
                existingRule.id(),
                id,
                nextRuleType,
                requireFeatureRuleValue(nextRuleType, nextRuleValue),
                nextSortOrder,
                active == null ? existingRule.active() : active
        );
        List<FeatureRule> newFeatureRules = featureRules.stream()
                .map(rule -> rule.id().equals(ruleId) ? updatedRule : rule)
                .toList();
        return new ConfigEntry(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                now,
                overrides,
                newFeatureRules
        );
    }

    public ConfigEntryView toView() {
        return new ConfigEntryView(
                id,
                configKey,
                name,
                configType,
                defaultValue,
                validationRule,
                mutableAtRuntime,
                status,
                tenantAware,
                createdAt,
                updatedAt,
                overrides.stream().map(ConfigOverride::toView).toList(),
                featureRules.stream().map(FeatureRule::toView).toList()
        );
    }

    public int nextSortOrder() {
        return featureRules.stream()
                .map(FeatureRule::sortOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private static List<ConfigOverride> normalizeOverrides(UUID entryId, List<ConfigOverride> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return List.of();
        }
        return overrides.stream()
                .map(configOverride -> {
                    if (!entryId.equals(configOverride.configEntryId())) {
                        throw new IllegalArgumentException("override configEntryId does not match aggregate id");
                    }
                    return configOverride;
                })
                .sorted(Comparator
                        .comparingInt((ConfigOverride configOverride) ->
                                OVERRIDE_PRECEDENCE.indexOf(configOverride.scopeType()))
                        .thenComparing(ConfigOverride::scopeId))
                .toList();
    }

    private static List<FeatureRule> normalizeFeatureRules(UUID entryId, List<FeatureRule> featureRules) {
        if (featureRules == null || featureRules.isEmpty()) {
            return List.of();
        }
        return featureRules.stream()
                .map(featureRule -> {
                    if (!entryId.equals(featureRule.configEntryId())) {
                        throw new IllegalArgumentException("feature rule configEntryId does not match aggregate id");
                    }
                    return featureRule;
                })
                .sorted(Comparator.comparingInt(FeatureRule::sortOrder).thenComparing(FeatureRule::id))
                .toList();
    }

    private static List<ConfigOverride> appendOverride(List<ConfigOverride> currentOverrides, ConfigOverride configOverride) {
        java.util.ArrayList<ConfigOverride> items = new java.util.ArrayList<>(currentOverrides);
        items.add(configOverride);
        items.sort(Comparator
                .comparingInt((ConfigOverride item) -> OVERRIDE_PRECEDENCE.indexOf(item.scopeType()))
                .thenComparing(ConfigOverride::scopeId));
        return List.copyOf(items);
    }

    private static List<FeatureRule> appendFeatureRule(List<FeatureRule> currentRules, FeatureRule featureRule) {
        java.util.ArrayList<FeatureRule> items = new java.util.ArrayList<>(currentRules);
        items.add(featureRule);
        items.sort(Comparator.comparingInt(FeatureRule::sortOrder).thenComparing(FeatureRule::id));
        return List.copyOf(items);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " length must be <= " + maxLength);
        }
        return normalized;
    }

    private static String requireConfigValue(ConfigType configType, String value, String validationRule) {
        Objects.requireNonNull(value, "value must not be null");
        switch (configType) {
            case STRING -> validateStringValue(value, validationRule);
            case NUMBER -> validateNumberValue(value, validationRule);
            case BOOLEAN -> validateBooleanValue(value, validationRule);
            case JSON -> validateJsonValue(value);
            case FEATURE_FLAG -> validateFeatureFlagValue(value);
            default -> throw new IllegalArgumentException("unsupported config type: " + configType);
        }
        return value;
    }

    private static void validateStringValue(String value, String validationRule) {
        if (validationRule == null || validationRule.isBlank()) {
            return;
        }
        JsonNode rule = parseJsonIfPossible(validationRule);
        if (rule == null || !rule.isObject()) {
            return;
        }
        Integer minLength = readInt(rule, "minLength");
        Integer maxLength = readInt(rule, "maxLength");
        String regex = readText(rule, "regex");
        if (minLength != null && value.length() < minLength) {
            throw new IllegalArgumentException("value length is below minLength");
        }
        if (maxLength != null && value.length() > maxLength) {
            throw new IllegalArgumentException("value length exceeds maxLength");
        }
        if (regex != null) {
            try {
                if (!Pattern.compile(regex).matcher(value).matches()) {
                    throw new IllegalArgumentException("value does not match validation regex");
                }
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException("validation regex is invalid", ex);
            }
        }
        JsonNode allowedValues = rule.get("allowedValues");
        if (allowedValues != null && allowedValues.isArray()) {
            boolean matched = false;
            for (JsonNode candidate : allowedValues) {
                if (Objects.equals(candidate.asText(), value)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("value is not in allowedValues");
            }
        }
    }

    private static void validateNumberValue(String value, String validationRule) {
        String normalized = requireNonBlank(value, "numberValue");
        BigDecimal number = parseBigDecimal(normalized);
        JsonNode rule = parseJsonIfPossible(validationRule);
        if (rule == null || !rule.isObject()) {
            return;
        }
        BigDecimal min = readDecimal(rule, "min");
        BigDecimal max = readDecimal(rule, "max");
        if (min != null && number.compareTo(min) < 0) {
            throw new IllegalArgumentException("number value is below min");
        }
        if (max != null && number.compareTo(max) > 0) {
            throw new IllegalArgumentException("number value is above max");
        }
    }

    private static void validateBooleanValue(String value, String validationRule) {
        String normalized = requireNonBlank(value, "booleanValue");
        if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("boolean value must be true or false");
        }
        JsonNode rule = parseJsonIfPossible(validationRule);
        if (rule == null || !rule.isObject()) {
            return;
        }
        JsonNode allowedValues = rule.get("allowedValues");
        if (allowedValues != null && allowedValues.isArray()) {
            boolean matched = false;
            for (JsonNode candidate : allowedValues) {
                if (normalized.equalsIgnoreCase(candidate.asText())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("boolean value is not in allowedValues");
            }
        }
    }

    private static void validateJsonValue(String value) {
        String normalized = requireNonBlank(value, "jsonValue");
        try {
            OBJECT_MAPPER.readTree(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("json value is invalid", ex);
        }
    }

    private static void validateFeatureFlagValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("feature flag value must not be null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("feature flag value must not be blank");
        }
        if ("true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
            return;
        }
        validateJsonValue(normalized);
    }

    private static String requireFeatureRuleValue(FeatureRuleType ruleType, String ruleValue) {
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        String normalized = requireNonBlank(ruleValue, "ruleValue");
        JsonNode root = parseJsonIfPossible(normalized);
        if (ruleType == FeatureRuleType.GLOBAL) {
            validateFeatureFlagValue(normalized);
            return normalized;
        }
        if (ruleType == FeatureRuleType.PERCENTAGE) {
            if (root != null && root.isObject()) {
                Integer percentage = readInt(root, "percentage");
                if (percentage == null || percentage < 0 || percentage > 100) {
                    throw new IllegalArgumentException("percentage rule requires percentage between 0 and 100");
                }
            } else {
                int percentage = Integer.parseInt(normalized);
                if (percentage < 0 || percentage > 100) {
                    throw new IllegalArgumentException("percentage rule requires percentage between 0 and 100");
                }
            }
            return normalized;
        }
        if (root == null) {
            return normalized;
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("scoped feature rule must be a scalar scope id or json object");
        }
        List<String> idFields = switch (ruleType) {
            case TENANT -> List.of("scopeId", "tenantId", "id");
            case ORG -> List.of("scopeId", "orgId", "organizationId", "id");
            case ROLE -> List.of("scopeId", "roleId", "id");
            case USER -> List.of("scopeId", "userId", "personId", "id");
            default -> List.of("scopeId", "id");
        };
        String scopeId = null;
        for (String idField : idFields) {
            scopeId = readText(root, idField);
            if (scopeId != null) {
                break;
            }
        }
        if (scopeId == null) {
            throw new IllegalArgumentException("scoped feature rule json requires scope id");
        }
        String enabled = firstNonBlank(readText(root, "enabled"), readText(root, "value"));
        if (enabled != null) {
            validateFeatureFlagValue(enabled);
        }
        return normalized;
    }

    private static String normalizeOptionalStructuredValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        parseJsonIfPossible(normalized);
        return normalized;
    }

    private static JsonNode parseJsonIfPossible(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (!looksLikeJson(normalized)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    private static boolean looksLikeJson(String value) {
        return value.startsWith("{")
                || value.startsWith("[")
                || value.startsWith("\"")
                || "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value)
                || Character.isDigit(value.charAt(0))
                || value.charAt(0) == '-';
    }

    private static Integer readInt(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asInt();
    }

    private static BigDecimal readDecimal(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return parseBigDecimal(node.asText());
    }

    private static String readText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid numeric value", ex);
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
