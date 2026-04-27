package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DeliveryTask(
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
        List<DeliveryAttempt> attempts
) {

    public DeliveryTask {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(channelType, "channelType must not be null");
        if (routeOrder < 0 || retryCount < 0) {
            throw new IllegalArgumentException("routeOrder and retryCount must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        attempts = attempts == null ? List.of() : List.copyOf(attempts);
    }

    public static DeliveryTask create(
            UUID id,
            UUID notificationId,
            ChannelType channelType,
            UUID endpointId,
            int routeOrder,
            UUID tenantId,
            Instant now
    ) {
        return new DeliveryTask(
                id,
                notificationId,
                channelType,
                endpointId,
                routeOrder,
                DeliveryTaskStatus.PENDING,
                0,
                now,
                null,
                null,
                null,
                null,
                tenantId,
                now,
                now,
                List.of()
        );
    }

    public DeliveryTask markDelivered(String providerMessageId, DeliveryAttempt attempt, Instant now) {
        return withState(
                DeliveryTaskStatus.DELIVERED,
                retryCount,
                null,
                providerMessageId,
                null,
                null,
                now,
                appendAttempt(attempt),
                now
        );
    }

    public DeliveryTask markFailed(
            DeliveryAttempt attempt,
            int maxRetryCount,
            Instant nextRetryAt,
            String errorCode,
            String errorMessage,
            Instant now
    ) {
        int updatedRetryCount = retryCount + 1;
        DeliveryTaskStatus targetStatus = updatedRetryCount >= maxRetryCount
                ? DeliveryTaskStatus.GAVE_UP
                : DeliveryTaskStatus.FAILED;
        Instant targetRetryAt = targetStatus == DeliveryTaskStatus.GAVE_UP ? null : nextRetryAt;
        return withState(
                targetStatus,
                updatedRetryCount,
                targetRetryAt,
                providerMessageId,
                errorCode,
                errorMessage,
                deliveredAt,
                appendAttempt(attempt),
                now
        );
    }

    public DeliveryTaskView toView() {
        return new DeliveryTaskView(
                id,
                notificationId,
                channelType,
                endpointId,
                routeOrder,
                status,
                retryCount,
                nextRetryAt,
                providerMessageId,
                lastErrorCode,
                lastErrorMessage,
                deliveredAt,
                tenantId,
                createdAt,
                updatedAt,
                attempts.stream().map(DeliveryAttempt::toView).toList()
        );
    }

    private List<DeliveryAttempt> appendAttempt(DeliveryAttempt attempt) {
        if (attempt == null) {
            return attempts;
        }
        java.util.ArrayList<DeliveryAttempt> updated = new java.util.ArrayList<>(attempts);
        updated.add(attempt);
        return updated;
    }

    private DeliveryTask withState(
            DeliveryTaskStatus targetStatus,
            int targetRetryCount,
            Instant targetNextRetryAt,
            String targetProviderMessageId,
            String targetLastErrorCode,
            String targetLastErrorMessage,
            Instant targetDeliveredAt,
            List<DeliveryAttempt> targetAttempts,
            Instant now
    ) {
        return new DeliveryTask(
                id,
                notificationId,
                channelType,
                endpointId,
                routeOrder,
                targetStatus,
                targetRetryCount,
                targetNextRetryAt,
                targetProviderMessageId,
                targetLastErrorCode,
                targetLastErrorMessage,
                targetDeliveredAt,
                tenantId,
                createdAt,
                now,
                targetAttempts
        );
    }
}
