package com.hjo2oa.infra.config.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.config.domain.ConfigEntry;
import com.hjo2oa.infra.config.domain.ConfigEntryRepository;
import com.hjo2oa.infra.config.domain.ConfigEntryView;
import com.hjo2oa.infra.config.domain.ConfigOverride;
import com.hjo2oa.infra.config.domain.ConfigStatus;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.ConfigUpdatedEvent;
import com.hjo2oa.infra.config.domain.FeatureFlagChangedEvent;
import com.hjo2oa.infra.config.domain.FeatureRule;
import com.hjo2oa.infra.config.domain.FeatureRuleType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import com.hjo2oa.infra.config.domain.ResolvedConfigValueView;
import com.hjo2oa.infra.config.domain.ResolvedValueSourceType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigEntryApplicationService {

    private final ConfigEntryRepository repository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<ResolutionCacheKey, ResolvedConfigValueView> resolutionCache = new ConcurrentHashMap<>();
    @Autowired
    public ConfigEntryApplicationService(
            ConfigEntryRepository repository,
            DomainEventPublisher domainEventPublisher,
            ObjectMapper objectMapper
    ) {
        this(repository, domainEventPublisher, objectMapper, Clock.systemUTC());
    }

    public ConfigEntryApplicationService(
            ConfigEntryRepository repository,
            DomainEventPublisher domainEventPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ConfigEntryView createEntry(
            String key,
            String name,
            ConfigType type,
            String defaultValue,
            boolean mutableAtRuntime,
            boolean tenantAware,
            String validationRule
    ) {
        ensureUniqueKey(key, null);
        ConfigEntry entry = ConfigEntry.create(
                key,
                name,
                type,
                defaultValue,
                mutableAtRuntime,
                tenantAware,
                validationRule,
                now()
        );
        repository.save(entry);
        invalidateCache(entry.configKey());
        return entry.toView();
    }

    public ConfigEntryView createEntry(ConfigEntryCommands.CreateEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return createEntry(
                command.configKey(),
                command.name(),
                command.configType(),
                command.defaultValue(),
                command.mutableAtRuntime(),
                command.tenantAware(),
                command.validationRule()
        );
    }

    public ConfigEntryView disableEntry(UUID entryId) {
        ConfigEntry entry = loadRequiredEntry(entryId);
        ConfigEntry updated = entry.disable(now());
        repository.save(updated);
        if (updated != entry) {
            invalidateCache(updated.configKey());
            domainEventPublisher.publish(ConfigUpdatedEvent.forDefaultUpdate(updated, "DISABLED", now()));
        }
        return updated.toView();
    }

    public ConfigEntryView updateDefault(UUID entryId, String defaultValue) {
        ConfigEntry entry = loadRequiredEntry(entryId);
        if (entry.status() == ConfigStatus.ACTIVE && !entry.mutableAtRuntime()) {
            throw new BizException(
                    SharedErrorDescriptors.CONFLICT,
                    "Runtime mutation is forbidden for this config entry"
            );
        }
        ConfigEntry updated = entry.updateDefault(defaultValue, now());
        repository.save(updated);
        if (updated != entry) {
            invalidateCache(updated.configKey());
            domainEventPublisher.publish(ConfigUpdatedEvent.forDefaultUpdate(updated, "DEFAULT_UPDATED", now()));
        }
        return updated.toView();
    }

    public ConfigEntryView updateDefault(ConfigEntryCommands.UpdateDefaultCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return updateDefault(command.entryId(), command.defaultValue());
    }

    public ConfigEntryView addOverride(
            UUID entryId,
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue
    ) {
        ConfigEntry entry = loadRequiredEntry(entryId);
        if (scopeType == OverrideScopeType.TENANT && !entry.tenantAware()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Tenant scoped override is not supported for this config entry"
            );
        }
        if (entry.overrides().stream().anyMatch(existing -> existing.scopeType() == scopeType
                && existing.scopeId().equals(scopeId))) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Override already exists for the given scope");
        }
        ConfigEntry updated = entry.addOverride(scopeType, scopeId, overrideValue, now());
        repository.save(updated);
        invalidateCache(updated.configKey());
        domainEventPublisher.publish(ConfigUpdatedEvent.forScopedUpdate(
                updated,
                "OVERRIDE_ADDED",
                scopeType,
                scopeId,
                now()
        ));
        return updated.toView();
    }

    public ConfigEntryView addOverride(ConfigEntryCommands.AddOverrideCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return addOverride(command.entryId(), command.scopeType(), command.scopeId(), command.overrideValue());
    }

    public ConfigEntryView removeOverride(UUID entryId, UUID overrideId) {
        ConfigEntry entry = loadRequiredEntry(entryId);
        ConfigOverride override = entry.overrides().stream()
                .filter(candidate -> candidate.id().equals(overrideId))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Config override not found"
                ));
        ConfigEntry updated = entry.removeOverride(overrideId, now());
        repository.save(updated);
        invalidateCache(updated.configKey());
        domainEventPublisher.publish(ConfigUpdatedEvent.forScopedUpdate(
                updated,
                "OVERRIDE_REMOVED",
                override.scopeType(),
                override.scopeId(),
                now()
        ));
        return updated.toView();
    }

    public ConfigEntryView disableOverride(UUID overrideId) {
        Objects.requireNonNull(overrideId, "overrideId must not be null");
        ConfigEntry entry = repository.findAll().stream()
                .filter(candidate -> candidate.overrides().stream()
                        .anyMatch(configOverride -> configOverride.id().equals(overrideId)))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Config override not found"
                ));
        ConfigOverride override = entry.overrides().stream()
                .filter(candidate -> candidate.id().equals(overrideId))
                .findFirst()
                .orElseThrow();
        ConfigEntry updated = entry.deactivateOverride(overrideId, now());
        repository.save(updated);
        invalidateCache(updated.configKey());
        domainEventPublisher.publish(ConfigUpdatedEvent.forScopedUpdate(
                updated,
                "OVERRIDE_DISABLED",
                override.scopeType(),
                override.scopeId(),
                now()
        ));
        return updated.toView();
    }

    public ConfigEntryView addFeatureRule(
            UUID entryId,
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder
    ) {
        ConfigEntry entry = loadRequiredEntry(entryId);
        if (entry.configType() != ConfigType.FEATURE_FLAG) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Only feature flag configs can define feature rules"
            );
        }
        int normalizedSortOrder = sortOrder == null ? entry.nextSortOrder() : sortOrder;
        if (entry.featureRules().stream().anyMatch(rule -> rule.sortOrder() == normalizedSortOrder)) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Feature rule sort order already exists");
        }
        ConfigEntry updated = entry.addFeatureRule(ruleType, ruleValue, normalizedSortOrder, now());
        FeatureRule featureRule = updated.featureRules().stream()
                .filter(rule -> rule.sortOrder() == normalizedSortOrder)
                .findFirst()
                .orElseThrow();
        repository.save(updated);
        invalidateCache(updated.configKey());
        domainEventPublisher.publish(FeatureFlagChangedEvent.from(
                updated,
                featureRule,
                "RULE_ADDED",
                now()
        ));
        return updated.toView();
    }

    public ConfigEntryView addFeatureRule(ConfigEntryCommands.AddFeatureRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return addFeatureRule(command.entryId(), command.ruleType(), command.ruleValue(), command.sortOrder());
    }

    public ConfigEntryView updateFeatureRule(
            UUID ruleId,
            FeatureRuleType ruleType,
            String ruleValue,
            Integer sortOrder,
            Boolean active
    ) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        ConfigEntry entry = repository.findAll().stream()
                .filter(candidate -> candidate.featureRules().stream().anyMatch(rule -> rule.id().equals(ruleId)))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Feature rule not found"
                ));
        FeatureRule existingRule = entry.featureRules().stream()
                .filter(rule -> rule.id().equals(ruleId))
                .findFirst()
                .orElseThrow();
        ConfigEntry updated;
        try {
            updated = entry.updateFeatureRule(ruleId, ruleType, ruleValue, sortOrder, active, now());
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
        FeatureRule updatedRule = updated.featureRules().stream()
                .filter(rule -> rule.id().equals(ruleId))
                .findFirst()
                .orElseThrow();
        repository.save(updated);
        invalidateCache(updated.configKey());
        domainEventPublisher.publish(FeatureFlagChangedEvent.from(
                updated,
                updatedRule,
                existingRule.active() && !updatedRule.active() ? "RULE_DISABLED" : "RULE_UPDATED",
                now()
        ));
        return updated.toView();
    }

    public ResolvedConfigValueView resolveValue(
            String key,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
        String normalizedKey = normalizeRequiredKey(key);
        ResolutionCacheKey cacheKey = new ResolutionCacheKey(normalizedKey, tenantId, orgId, roleId, userId);
        ResolvedConfigValueView cachedValue = resolutionCache.get(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }
        ConfigEntry entry = repository.findByKey(normalizedKey)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Config entry not found"
                ));
        if (entry.status() == ConfigStatus.DISABLED) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Config entry is disabled"
            );
        }

        String resolvedValue = entry.defaultValue();
        ResolvedValueSourceType sourceType = ResolvedValueSourceType.DEFAULT;
        UUID overrideId = null;
        UUID featureRuleId = null;
        List<String> trace = new ArrayList<>();
        trace.add("DEFAULT");

        OverrideMatch overrideMatch = resolveOverride(entry, tenantId, orgId, roleId, userId);
        if (overrideMatch != null) {
            resolvedValue = overrideMatch.overrideValue();
            sourceType = ResolvedValueSourceType.OVERRIDE;
            overrideId = overrideMatch.overrideId();
            trace.add("OVERRIDE:" + overrideMatch.scopeType().name() + ":" + overrideMatch.scopeId());
        }

        if (entry.configType() == ConfigType.FEATURE_FLAG) {
            FeatureRuleMatch featureRuleMatch = resolveFeatureRule(entry, tenantId, orgId, roleId, userId);
            if (featureRuleMatch != null) {
                resolvedValue = featureRuleMatch.resolvedValue();
                sourceType = ResolvedValueSourceType.FEATURE_RULE;
                featureRuleId = featureRuleMatch.ruleId();
                trace.add("FEATURE_RULE:" + featureRuleMatch.ruleType().name() + ":" + featureRuleMatch.ruleId());
            }
        }

        ResolvedConfigValueView resolvedValueView = new ResolvedConfigValueView(
                entry.id(),
                entry.configKey(),
                entry.configType(),
                entry.status(),
                resolvedValue,
                sourceType,
                overrideId,
                featureRuleId,
                tenantId,
                orgId,
                roleId,
                userId,
                trace
        );
        resolutionCache.put(cacheKey, resolvedValueView);
        return resolvedValueView;
    }

    public ResolvedConfigValueView resolveValue(ConfigEntryCommands.ResolveValueQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return resolveValue(query.key(), query.tenantId(), query.orgId(), query.roleId(), query.userId());
    }

    public Optional<ConfigEntryView> queryByKey(String key) {
        return repository.findByKey(normalizeRequiredKey(key)).map(ConfigEntry::toView);
    }

    public List<ConfigEntryView> list(ConfigEntryCommands.ListQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return repository.findAll().stream()
                .filter(entry -> query.configKey() == null || entry.configKey().equals(query.configKey().trim()))
                .filter(entry -> query.keyword() == null
                        || query.keyword().isBlank()
                        || entry.configKey().contains(query.keyword().trim())
                        || entry.name().contains(query.keyword().trim()))
                .filter(entry -> query.configType() == null || entry.configType() == query.configType())
                .filter(entry -> query.status() == null || entry.status() == query.status())
                .filter(entry -> query.tenantAware() == null || entry.tenantAware() == query.tenantAware())
                .filter(entry -> query.mutableAtRuntime() == null
                        || entry.mutableAtRuntime() == query.mutableAtRuntime())
                .sorted(Comparator.comparing(ConfigEntry::updatedAt).reversed().thenComparing(ConfigEntry::configKey))
                .map(ConfigEntry::toView)
                .toList();
    }

    private ConfigEntry loadRequiredEntry(UUID entryId) {
        Objects.requireNonNull(entryId, "entryId must not be null");
        return repository.findById(entryId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Config entry not found"
                ));
    }

    private void ensureUniqueKey(String key, UUID entryId) {
        repository.findByKey(normalizeRequiredKey(key))
                .filter(existing -> !existing.id().equals(entryId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Config key already exists");
                });
    }

    private OverrideMatch resolveOverride(
            ConfigEntry entry,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
        OverrideMatch latestMatch = null;
        latestMatch = findOverrideMatch(entry, OverrideScopeType.TENANT, tenantId, latestMatch);
        latestMatch = findOverrideMatch(entry, OverrideScopeType.ORGANIZATION, orgId, latestMatch);
        latestMatch = findOverrideMatch(entry, OverrideScopeType.ROLE, roleId, latestMatch);
        latestMatch = findOverrideMatch(entry, OverrideScopeType.USER, userId, latestMatch);
        return latestMatch;
    }

    private OverrideMatch findOverrideMatch(
            ConfigEntry entry,
            OverrideScopeType scopeType,
            UUID scopeId,
            OverrideMatch currentMatch
    ) {
        if (scopeId == null) {
            return currentMatch;
        }
        return entry.overrides().stream()
                .filter(ConfigOverride::active)
                .filter(override -> override.scopeType() == scopeType)
                .filter(override -> override.scopeId().equals(scopeId))
                .findFirst()
                .map(override -> new OverrideMatch(
                        override.id(),
                        override.scopeType(),
                        override.scopeId(),
                        override.overrideValue()
                ))
                .orElse(currentMatch);
    }

    private FeatureRuleMatch resolveFeatureRule(
            ConfigEntry entry,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
        for (FeatureRule featureRule : entry.featureRules()) {
            if (!featureRule.active()) {
                continue;
            }
            String resolvedValue = evaluateFeatureRule(featureRule, entry.configKey(), tenantId, orgId, roleId, userId);
            if (resolvedValue != null) {
                return new FeatureRuleMatch(featureRule.id(), featureRule.ruleType(), resolvedValue);
            }
        }
        return null;
    }

    private String evaluateFeatureRule(
            FeatureRule featureRule,
            String configKey,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
        return switch (featureRule.ruleType()) {
            case GLOBAL -> resolveConfiguredValue(featureRule.ruleValue(), "true");
            case TENANT -> matchScopedRule(featureRule.ruleValue(), tenantId, List.of("scopeId", "tenantId", "id"));
            case ORG -> matchScopedRule(featureRule.ruleValue(), orgId, List.of("scopeId", "orgId", "id"));
            case ROLE -> matchScopedRule(featureRule.ruleValue(), roleId, List.of("scopeId", "roleId", "id"));
            case USER -> matchScopedRule(featureRule.ruleValue(), userId, List.of("scopeId", "userId", "personId", "id"));
            case PERCENTAGE -> matchPercentageRule(featureRule.ruleValue(), configKey, tenantId, orgId, roleId, userId);
        };
    }

    private String matchScopedRule(String rawValue, UUID scopeId, List<String> idFields) {
        if (scopeId == null) {
            return null;
        }
        String normalized = rawValue == null ? null : rawValue.trim();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        JsonNode jsonNode = tryReadJson(normalized);
        if (jsonNode == null) {
            return normalized.equals(scopeId.toString()) ? "true" : null;
        }
        if (!jsonNode.isObject()) {
            return null;
        }
        String configuredScopeId = null;
        for (String idField : idFields) {
            configuredScopeId = readText(jsonNode, idField);
            if (configuredScopeId != null) {
                break;
            }
        }
        if (configuredScopeId == null || !configuredScopeId.equals(scopeId.toString())) {
            return null;
        }
        return resolveConfiguredValue(normalized, "true");
    }

    private String matchPercentageRule(
            String rawValue,
            String configKey,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
        String subject = firstNonNull(userId, roleId, orgId, tenantId);
        if (subject == null) {
            return null;
        }
        String normalized = rawValue == null ? null : rawValue.trim();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        JsonNode jsonNode = tryReadJson(normalized);
        int percentage;
        String salt = configKey;
        if (jsonNode != null && jsonNode.isObject()) {
            JsonNode percentageNode = jsonNode.get("percentage");
            if (percentageNode == null || percentageNode.isNull()) {
                return null;
            }
            percentage = percentageNode.asInt(-1);
            String configuredSalt = readText(jsonNode, "salt");
            if (configuredSalt != null) {
                salt = configuredSalt;
            }
        } else {
            percentage = Integer.parseInt(normalized);
        }
        if (percentage < 0 || percentage > 100) {
            return null;
        }
        int bucket = Math.floorMod((subject + ":" + salt).hashCode(), 100);
        if (bucket >= percentage) {
            return null;
        }
        return resolveConfiguredValue(normalized, "true");
    }

    private String resolveConfiguredValue(String rawValue, String defaultValue) {
        String normalized = rawValue == null ? null : rawValue.trim();
        if (normalized == null || normalized.isEmpty()) {
            return defaultValue;
        }
        JsonNode jsonNode = tryReadJson(normalized);
        if (jsonNode == null || !jsonNode.isObject()) {
            return normalized;
        }
        String enabled = readText(jsonNode, "enabled");
        if (enabled != null) {
            return enabled;
        }
        JsonNode valueNode = jsonNode.get("value");
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.isValueNode() ? valueNode.asText() : valueNode.toString();
    }

    private JsonNode tryReadJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        char first = rawValue.charAt(0);
        if (first != '{' && first != '[' && first != '"' && first != '-' && !Character.isDigit(first)
                && !"true".equalsIgnoreCase(rawValue)
                && !"false".equalsIgnoreCase(rawValue)
                && !"null".equalsIgnoreCase(rawValue)) {
            return null;
        }
        try {
            return objectMapper.readTree(rawValue);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readText(JsonNode jsonNode, String fieldName) {
        JsonNode field = jsonNode.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequiredKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String normalized = key.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return normalized;
    }

    private String firstNonNull(UUID userId, UUID roleId, UUID orgId, UUID tenantId) {
        if (userId != null) {
            return userId.toString();
        }
        if (roleId != null) {
            return roleId.toString();
        }
        if (orgId != null) {
            return orgId.toString();
        }
        return tenantId == null ? null : tenantId.toString();
    }

    private Instant now() {
        return clock.instant();
    }

    private void invalidateCache(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            resolutionCache.clear();
            return;
        }
        resolutionCache.keySet().removeIf(key -> key.configKey().equals(configKey));
    }

    private record OverrideMatch(
            UUID overrideId,
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue
    ) {
    }

    private record FeatureRuleMatch(
            UUID ruleId,
            FeatureRuleType ruleType,
            String resolvedValue
    ) {
    }

    private record ResolutionCacheKey(
            String configKey,
            UUID tenantId,
            UUID orgId,
            UUID roleId,
            UUID userId
    ) {
    }
}
