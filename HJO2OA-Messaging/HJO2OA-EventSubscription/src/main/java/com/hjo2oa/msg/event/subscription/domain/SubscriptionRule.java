package com.hjo2oa.msg.event.subscription.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record SubscriptionRule(
        UUID id,
        String ruleCode,
        String eventTypePattern,
        NotificationCategory notificationCategory,
        TargetResolverType targetResolverType,
        String targetResolverConfig,
        String templateCode,
        String conditionExpr,
        String priorityMapping,
        NotificationPriority defaultPriority,
        boolean enabled,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public SubscriptionRule {
        Objects.requireNonNull(id, "id must not be null");
        ruleCode = requireText(ruleCode, "ruleCode");
        eventTypePattern = requireText(eventTypePattern, "eventTypePattern");
        Objects.requireNonNull(notificationCategory, "notificationCategory must not be null");
        Objects.requireNonNull(targetResolverType, "targetResolverType must not be null");
        targetResolverConfig = normalizeNullable(targetResolverConfig);
        templateCode = requireText(templateCode, "templateCode");
        conditionExpr = normalizeNullable(conditionExpr);
        priorityMapping = normalizeNullable(priorityMapping);
        defaultPriority = defaultPriority == null ? NotificationPriority.NORMAL : defaultPriority;
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SubscriptionRule create(
            UUID id,
            String ruleCode,
            String eventTypePattern,
            NotificationCategory notificationCategory,
            TargetResolverType targetResolverType,
            String targetResolverConfig,
            String templateCode,
            String conditionExpr,
            String priorityMapping,
            NotificationPriority defaultPriority,
            boolean enabled,
            UUID tenantId,
            Instant now
    ) {
        return new SubscriptionRule(
                id,
                ruleCode,
                eventTypePattern,
                notificationCategory,
                targetResolverType,
                targetResolverConfig,
                templateCode,
                conditionExpr,
                priorityMapping,
                defaultPriority,
                enabled,
                tenantId,
                now,
                now
        );
    }

    public SubscriptionRule update(
            String newEventTypePattern,
            NotificationCategory newNotificationCategory,
            TargetResolverType newTargetResolverType,
            String newTargetResolverConfig,
            String newTemplateCode,
            String newConditionExpr,
            String newPriorityMapping,
            NotificationPriority newDefaultPriority,
            boolean newEnabled,
            Instant now
    ) {
        return new SubscriptionRule(
                id,
                ruleCode,
                newEventTypePattern,
                newNotificationCategory,
                newTargetResolverType,
                newTargetResolverConfig,
                newTemplateCode,
                newConditionExpr,
                newPriorityMapping,
                newDefaultPriority,
                newEnabled,
                tenantId,
                createdAt,
                now
        );
    }

    public SubscriptionRule toggle(boolean newEnabled, Instant now) {
        if (enabled == newEnabled) {
            return this;
        }
        return new SubscriptionRule(
                id,
                ruleCode,
                eventTypePattern,
                notificationCategory,
                targetResolverType,
                targetResolverConfig,
                templateCode,
                conditionExpr,
                priorityMapping,
                defaultPriority,
                newEnabled,
                tenantId,
                createdAt,
                now
        );
    }

    public boolean matchesEventType(String eventType) {
        String normalizedEventType = requireText(eventType, "eventType");
        if (eventTypePattern.equals(normalizedEventType)) {
            return true;
        }
        String regex = Pattern.quote(eventTypePattern).replace("*", "\\E.*\\Q");
        return Pattern.matches(regex, normalizedEventType);
    }

    public SubscriptionRuleView toView() {
        return new SubscriptionRuleView(
                id,
                ruleCode,
                eventTypePattern,
                notificationCategory,
                targetResolverType,
                targetResolverConfig,
                templateCode,
                conditionExpr,
                priorityMapping,
                defaultPriority,
                enabled,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
