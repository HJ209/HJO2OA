package com.hjo2oa.msg.message.center.domain;

import java.time.Instant;
import java.util.Objects;

public record Notification(
        String notificationId,
        String dedupKey,
        String tenantId,
        String recipientId,
        String targetAssignmentId,
        String targetPositionId,
        String title,
        String bodySummary,
        String deepLink,
        NotificationCategory category,
        NotificationPriority priority,
        NotificationInboxStatus inboxStatus,
        String sourceModule,
        String sourceEventType,
        String sourceBusinessId,
        Instant createdAt,
        Instant updatedAt,
        Instant readAt,
        Instant archivedAt,
        Instant revokedAt,
        Instant expiredAt,
        String statusReason
) {

    public Notification {
        notificationId = requireText(notificationId, "notificationId");
        dedupKey = requireText(dedupKey, "dedupKey");
        tenantId = requireText(tenantId, "tenantId");
        recipientId = requireText(recipientId, "recipientId");
        targetAssignmentId = normalize(targetAssignmentId);
        targetPositionId = normalize(targetPositionId);
        title = requireText(title, "title");
        bodySummary = requireText(bodySummary, "bodySummary");
        deepLink = requireText(deepLink, "deepLink");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(inboxStatus, "inboxStatus must not be null");
        sourceModule = requireText(sourceModule, "sourceModule");
        sourceEventType = requireText(sourceEventType, "sourceEventType");
        sourceBusinessId = requireText(sourceBusinessId, "sourceBusinessId");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        statusReason = normalize(statusReason);
    }

    public static Notification create(
            String notificationId,
            String dedupKey,
            String tenantId,
            String recipientId,
            String targetAssignmentId,
            String targetPositionId,
            String title,
            String bodySummary,
            String deepLink,
            NotificationCategory category,
            NotificationPriority priority,
            String sourceModule,
            String sourceEventType,
            String sourceBusinessId,
            Instant createdAt
    ) {
        return new Notification(
                notificationId,
                dedupKey,
                tenantId,
                recipientId,
                targetAssignmentId,
                targetPositionId,
                title,
                bodySummary,
                deepLink,
                category,
                priority,
                NotificationInboxStatus.UNREAD,
                sourceModule,
                sourceEventType,
                sourceBusinessId,
                createdAt,
                createdAt,
                null,
                null,
                null,
                null,
                null
        );
    }

    public boolean isVisibleTo(MessageIdentityContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (!tenantId.equals(context.tenantId())) {
            return false;
        }
        if (!recipientId.equals(context.recipientId())) {
            return false;
        }
        if (targetAssignmentId != null && !targetAssignmentId.equals(context.assignmentId())) {
            return false;
        }
        return targetPositionId == null || targetPositionId.equals(context.positionId());
    }

    public boolean isUnread() {
        return inboxStatus == NotificationInboxStatus.UNREAD;
    }

    public Notification markRead(Instant readAt) {
        Objects.requireNonNull(readAt, "readAt must not be null");
        if (!isUnread()) {
            return this;
        }
        return new Notification(
                notificationId,
                dedupKey,
                tenantId,
                recipientId,
                targetAssignmentId,
                targetPositionId,
                title,
                bodySummary,
                deepLink,
                category,
                priority,
                NotificationInboxStatus.READ,
                sourceModule,
                sourceEventType,
                sourceBusinessId,
                createdAt,
                readAt,
                readAt,
                archivedAt,
                revokedAt,
                expiredAt,
                statusReason
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
