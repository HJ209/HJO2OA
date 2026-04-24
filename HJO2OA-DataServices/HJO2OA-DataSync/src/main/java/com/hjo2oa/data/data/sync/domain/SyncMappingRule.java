package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SyncMappingRule(
        UUID ruleId,
        UUID syncTaskId,
        String sourceField,
        String targetField,
        Map<String, Object> transformRule,
        ConflictStrategy conflictStrategy,
        boolean keyMapping,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {

    public SyncMappingRule {
        ruleId = SyncDomainSupport.requireId(ruleId, "ruleId");
        syncTaskId = SyncDomainSupport.requireId(syncTaskId, "syncTaskId");
        sourceField = SyncDomainSupport.requireText(sourceField, "sourceField");
        targetField = SyncDomainSupport.requireText(targetField, "targetField");
        transformRule = transformRule == null ? Map.of() : Map.copyOf(transformRule);
        Objects.requireNonNull(conflictStrategy, "conflictStrategy must not be null");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public Object mapValue(Map<String, Object> sourcePayload) {
        Object rawValue = sourcePayload == null ? null : sourcePayload.get(sourceField);
        Object value = rawValue;
        String operation = readRuleText("operation");
        if (operation != null) {
            value = applyOperation(operation, rawValue);
        }
        if (value == null && transformRule.containsKey("defaultValue")) {
            value = transformRule.get("defaultValue");
        }
        return value;
    }

    public SyncMappingRule withTaskId(UUID taskId) {
        return new SyncMappingRule(
                ruleId,
                SyncDomainSupport.requireId(taskId, "taskId"),
                sourceField,
                targetField,
                transformRule,
                conflictStrategy,
                keyMapping,
                sortOrder,
                createdAt,
                updatedAt
        );
    }

    private String readRuleText(String key) {
        Object value = transformRule.get(key);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Object applyOperation(String operation, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = operation.toUpperCase(Locale.ROOT);
        String textValue = String.valueOf(rawValue);
        return switch (normalized) {
            case "UPPER" -> textValue.toUpperCase(Locale.ROOT);
            case "LOWER" -> textValue.toLowerCase(Locale.ROOT);
            case "TRIM" -> textValue.trim();
            default -> {
                if (normalized.startsWith("PREFIX:")) {
                    yield operation.substring("PREFIX:".length()) + textValue;
                }
                if (normalized.startsWith("SUFFIX:")) {
                    yield textValue + operation.substring("SUFFIX:".length());
                }
                yield rawValue;
            }
        };
    }
}
