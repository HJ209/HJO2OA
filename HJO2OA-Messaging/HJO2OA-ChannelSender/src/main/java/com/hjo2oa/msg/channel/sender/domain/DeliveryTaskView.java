package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryTaskView(
        UUID id,
        UUID notificationId,
        ChannelType channelType,
        UUID endpointId,
        int routeOrder,
        DeliveryTaskStatus status,
        int retryCount,
        Instant nextRetryAt,
        String providerMessageId,
        String lastErrorCode,
        String lastErrorMessage,
        Instant deliveredAt,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryAttemptView> attempts
) {
}
