package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ChannelEndpoint(
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

    public ChannelEndpoint {
        Objects.requireNonNull(id, "id must not be null");
        endpointCode = MessageTemplate.requireText(endpointCode, "endpointCode");
        Objects.requireNonNull(channelType, "channelType must not be null");
        Objects.requireNonNull(providerType, "providerType must not be null");
        displayName = MessageTemplate.requireText(displayName, "displayName");
        Objects.requireNonNull(status, "status must not be null");
        configRef = MessageTemplate.requireText(configRef, "configRef");
        requireNonNegative(rateLimitPerMinute, "rateLimitPerMinute");
        requireNonNegative(dailyQuota, "dailyQuota");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ChannelEndpoint create(
            UUID id,
            String endpointCode,
            ChannelType channelType,
            ProviderType providerType,
            String displayName,
            String configRef,
            Integer rateLimitPerMinute,
            Integer dailyQuota,
            UUID tenantId,
            Instant now
    ) {
        return new ChannelEndpoint(
                id,
                endpointCode,
                channelType,
                providerType,
                displayName,
                ChannelEndpointStatus.ENABLED,
                configRef,
                rateLimitPerMinute,
                dailyQuota,
                tenantId,
                now,
                now
        );
    }

    public ChannelEndpoint withStatus(ChannelEndpointStatus targetStatus, Instant now) {
        return new ChannelEndpoint(
                id,
                endpointCode,
                channelType,
                providerType,
                displayName,
                targetStatus,
                configRef,
                rateLimitPerMinute,
                dailyQuota,
                tenantId,
                createdAt,
                now
        );
    }

    public ChannelEndpointView toView() {
        return new ChannelEndpointView(
                id,
                endpointCode,
                channelType,
                providerType,
                displayName,
                status,
                configRef,
                rateLimitPerMinute,
                dailyQuota,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private static void requireNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
