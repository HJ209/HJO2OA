package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RoutingPolicy(
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

    public RoutingPolicy {
        Objects.requireNonNull(id, "id must not be null");
        policyCode = MessageTemplate.requireText(policyCode, "policyCode");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(priorityThreshold, "priorityThreshold must not be null");
        targetChannelOrder = normalizeChannels(targetChannelOrder, true);
        fallbackChannelOrder = normalizeChannels(fallbackChannelOrder, false);
        Objects.requireNonNull(quietWindowBehavior, "quietWindowBehavior must not be null");
        if (dedupWindowSeconds < 0) {
            throw new IllegalArgumentException("dedupWindowSeconds must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static RoutingPolicy create(
            UUID id,
            String policyCode,
            MessageCategory category,
            MessagePriority priorityThreshold,
            List<ChannelType> targetChannelOrder,
            List<ChannelType> fallbackChannelOrder,
            QuietWindowBehavior quietWindowBehavior,
            int dedupWindowSeconds,
            String escalationPolicy,
            UUID tenantId,
            Instant now
    ) {
        return new RoutingPolicy(
                id,
                policyCode,
                category,
                priorityThreshold,
                targetChannelOrder,
                fallbackChannelOrder,
                quietWindowBehavior,
                dedupWindowSeconds,
                escalationPolicy,
                true,
                tenantId,
                now,
                now
        );
    }

    public RoutingPolicy enable(Instant now) {
        return withEnabled(true, now);
    }

    public RoutingPolicy disable(Instant now) {
        return withEnabled(false, now);
    }

    public RoutingPolicyView toView() {
        return new RoutingPolicyView(
                id,
                policyCode,
                category,
                priorityThreshold,
                targetChannelOrder,
                fallbackChannelOrder,
                quietWindowBehavior,
                dedupWindowSeconds,
                escalationPolicy,
                enabled,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private RoutingPolicy withEnabled(boolean targetEnabled, Instant now) {
        return new RoutingPolicy(
                id,
                policyCode,
                category,
                priorityThreshold,
                targetChannelOrder,
                fallbackChannelOrder,
                quietWindowBehavior,
                dedupWindowSeconds,
                escalationPolicy,
                targetEnabled,
                tenantId,
                createdAt,
                now
        );
    }

    private static List<ChannelType> normalizeChannels(List<ChannelType> channels, boolean required) {
        if (channels == null || channels.isEmpty()) {
            if (required) {
                return List.of(ChannelType.INBOX);
            }
            return List.of();
        }
        if (!channels.contains(ChannelType.INBOX)) {
            return java.util.stream.Stream.concat(java.util.stream.Stream.of(ChannelType.INBOX), channels.stream())
                    .distinct()
                    .toList();
        }
        return channels.stream().filter(Objects::nonNull).distinct().toList();
    }
}
