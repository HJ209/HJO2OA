package com.hjo2oa.msg.event.subscription.domain;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionRuleView(
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
}
