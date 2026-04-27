package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.UUID;

public record ChannelEndpointView(
        UUID id,
        String endpointCode,
        ChannelType channelType,
        ProviderType providerType,
        String displayName,
        ChannelEndpointStatus status,
        String configRef,
        Integer rateLimitPerMinute,
        Integer dailyQuota,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
