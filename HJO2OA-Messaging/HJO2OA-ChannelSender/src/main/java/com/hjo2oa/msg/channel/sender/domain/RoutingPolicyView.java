package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoutingPolicyView(
        UUID id,
        String policyCode,
        MessageCategory category,
        MessagePriority priorityThreshold,
        List<ChannelType> targetChannelOrder,
        List<ChannelType> fallbackChannelOrder,
        QuietWindowBehavior quietWindowBehavior,
        int dedupWindowSeconds,
        String escalationPolicy,
        boolean enabled,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
