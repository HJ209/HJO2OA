package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.MsgNotificationDeliveredEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationChannelDispatcher;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageNotificationCommandApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRecordRepository deliveryRecordRepository;
    private final List<NotificationChannelDispatcher> channelDispatchers;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Autowired
    public MessageNotificationCommandApplicationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRecordRepository deliveryRecordRepository,
            List<NotificationChannelDispatcher> channelDispatchers,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                notificationRepository,
                deliveryRecordRepository,
                channelDispatchers,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }

    public MessageNotificationCommandApplicationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRecordRepository deliveryRecordRepository,
            List<NotificationChannelDispatcher> channelDispatchers,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.deliveryRecordRepository = Objects.requireNonNull(deliveryRecordRepository);
        this.channelDispatchers = List.copyOf(channelDispatchers);
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher);
        this.clock = Objects.requireNonNull(clock);
    }

    public NotificationSummary createNotification(MessageNotificationCommands.CreateNotificationCommand command) {
        return createNotificationResult(command).notification();
    }

    public MessageNotificationCreateResult createNotificationResult(
            MessageNotificationCommands.CreateNotificationCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        return notificationRepository.findByDedupKey(command.dedupKey())
                .map(notification -> new MessageNotificationCreateResult(toSummary(notification), false))
                .orElseGet(() -> {
                    Notification notification = createAndDispatch(command);
                    return new MessageNotificationCreateResult(toSummary(notification), true);
                });
    }

    private Notification createAndDispatch(MessageNotificationCommands.CreateNotificationCommand command) {
        Instant occurredAt = command.occurredAt() == null ? clock.instant() : command.occurredAt();
        Notification notification = Notification.create(
                UUID.randomUUID().toString(),
                command.dedupKey(),
                command.tenantId(),
                command.recipientId(),
                command.targetAssignmentId(),
                command.targetPositionId(),
                command.title(),
                command.bodySummary(),
                command.deepLink(),
                command.category(),
                command.priority(),
                command.sourceModule(),
                command.sourceEventType(),
                command.sourceBusinessId(),
                occurredAt
        );
        Notification saved = notificationRepository.save(notification);
        dispatch(saved);
        return saved;
    }

    private void dispatch(Notification notification) {
        for (NotificationChannelDispatcher channelDispatcher : channelDispatchers) {
            for (NotificationDeliveryRecord deliveryRecord : channelDispatcher.dispatch(notification)) {
                deliveryRecordRepository.save(deliveryRecord);
                domainEventPublisher.publish(MsgNotificationSentEvent.from(notification, deliveryRecord));
                if (deliveryRecord.status() == NotificationDeliveryStatus.DELIVERED) {
                    domainEventPublisher.publish(MsgNotificationDeliveredEvent.from(notification, deliveryRecord));
                }
            }
        }
    }

    private NotificationSummary toSummary(Notification notification) {
        NotificationDeliveryStatus deliveryStatus = deliveryRecordRepository
                .findByNotificationId(notification.notificationId())
                .stream()
                .map(NotificationDeliveryRecord::status)
                .findFirst()
                .orElse(NotificationDeliveryStatus.PENDING);
        return new NotificationSummary(
                notification.notificationId(),
                notification.title(),
                notification.bodySummary(),
                notification.category(),
                notification.priority(),
                notification.inboxStatus(),
                deliveryStatus,
                notification.sourceModule(),
                notification.deepLink(),
                notification.targetAssignmentId(),
                notification.targetPositionId(),
                notification.createdAt()
        );
    }
}
