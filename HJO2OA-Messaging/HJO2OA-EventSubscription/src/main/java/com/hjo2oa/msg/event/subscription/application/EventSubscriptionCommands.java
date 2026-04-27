package com.hjo2oa.msg.event.subscription.application;

import com.hjo2oa.msg.event.subscription.domain.ChannelType;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.QuietWindow;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class EventSubscriptionCommands {

    private EventSubscriptionCommands() {
    }

    public record SaveRuleCommand(
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
            UUID tenantId
    ) {
    }

    public record UpdateRuleCommand(
            String eventTypePattern,
            NotificationCategory notificationCategory,
            TargetResolverType targetResolverType,
            String targetResolverConfig,
            String templateCode,
            String conditionExpr,
            String priorityMapping,
            NotificationPriority defaultPriority,
            boolean enabled
    ) {
    }

    public record ToggleRuleCommand(
            UUID ruleId,
            boolean enabled,
            String reason
    ) {
    }

    public record SavePreferenceCommand(
            UUID personId,
            NotificationCategory category,
            List<ChannelType> allowedChannels,
            QuietWindow quietWindow,
            DigestMode digestMode,
            boolean escalationOptIn,
            boolean muteNonWorkingHours,
            boolean enabled,
            UUID tenantId
    ) {
    }

    public record EventMatchQuery(
            String eventId,
            String eventType,
            UUID tenantId,
            UUID recipientPersonId,
            NotificationPriority eventPriority,
            LocalTime occurredLocalTime
    ) {
    }
}
