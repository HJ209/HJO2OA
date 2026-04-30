package com.hjo2oa.msg.channel.sender.infrastructure;

import com.hjo2oa.msg.channel.sender.application.ChannelSenderApplicationService;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskView;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationChannelDispatcher;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ChannelSenderNotificationChannelDispatcher implements NotificationChannelDispatcher {

    private final ChannelSenderApplicationService applicationService;

    public ChannelSenderNotificationChannelDispatcher(ChannelSenderApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public NotificationDeliveryChannel channel() {
        return NotificationDeliveryChannel.WEBHOOK;
    }

    @Override
    public List<NotificationDeliveryRecord> dispatch(Notification notification) {
        return applicationService.dispatchNotification(new ChannelSenderCommands.DispatchNotificationCommand(
                        toUuid(notification.notificationId()),
                        toUuid(notification.tenantId()),
                        notification.recipientId(),
                        notification.title(),
                        notification.bodySummary(),
                        notification.deepLink(),
                        mapCategory(notification.category()),
                        mapPriority(notification.priority()),
                        List.of()
                ))
                .stream()
                .map(task -> toDeliveryRecord(notification, task))
                .toList();
    }

    private NotificationDeliveryRecord toDeliveryRecord(Notification notification, DeliveryTaskView task) {
        return new NotificationDeliveryRecord(
                task.id().toString(),
                notification.notificationId(),
                mapChannel(task.channelType()),
                mapStatus(task.status()),
                task.retryCount(),
                task.createdAt(),
                task.updatedAt(),
                task.deliveredAt(),
                task.lastErrorCode()
        );
    }

    private MessageCategory mapCategory(com.hjo2oa.msg.message.center.domain.NotificationCategory category) {
        try {
            return MessageCategory.valueOf(category.name());
        } catch (IllegalArgumentException ex) {
            return MessageCategory.GENERAL;
        }
    }

    private MessagePriority mapPriority(com.hjo2oa.msg.message.center.domain.NotificationPriority priority) {
        return MessagePriority.valueOf(priority.name());
    }

    private NotificationDeliveryChannel mapChannel(ChannelType channelType) {
        return switch (channelType) {
            case INBOX -> NotificationDeliveryChannel.INBOX;
            case EMAIL -> NotificationDeliveryChannel.EMAIL;
            case SMS -> NotificationDeliveryChannel.SMS;
            case WEBHOOK, WECHAT_WORK, DINGTALK -> NotificationDeliveryChannel.WEBHOOK;
            case APP_PUSH -> NotificationDeliveryChannel.MOBILE_PUSH;
        };
    }

    private NotificationDeliveryStatus mapStatus(DeliveryTaskStatus status) {
        return switch (status) {
            case DELIVERED -> NotificationDeliveryStatus.DELIVERED;
            case GAVE_UP, FAILED -> NotificationDeliveryStatus.FAILED;
            default -> NotificationDeliveryStatus.PENDING;
        };
    }

    private UUID toUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
