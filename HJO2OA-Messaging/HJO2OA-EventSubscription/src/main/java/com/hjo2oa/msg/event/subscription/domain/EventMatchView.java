package com.hjo2oa.msg.event.subscription.domain;

import java.util.List;
import java.util.UUID;

public record EventMatchView(
        UUID ruleId,
        String ruleCode,
        String eventType,
        NotificationCategory notificationCategory,
        TargetResolverType targetResolverType,
        String templateCode,
        NotificationPriority priority,
        boolean quietNow,
        DigestMode digestMode,
        List<ChannelType> allowedChannels,
        boolean escalationAllowed
) {
}
