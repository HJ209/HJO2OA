package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.MsgNotificationDeliveredEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationChannelDispatcher;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationProjectionEventLog;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MessageNotificationProjectionApplicationService {

    private static final String SOURCE_MODULE = "todo-center";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRecordRepository deliveryRecordRepository;
    private final NotificationProjectionEventLog projectionEventLog;
    private final List<NotificationChannelDispatcher> channelDispatchers;
    private final DomainEventPublisher domainEventPublisher;

    public MessageNotificationProjectionApplicationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRecordRepository deliveryRecordRepository,
            NotificationProjectionEventLog projectionEventLog,
            List<NotificationChannelDispatcher> channelDispatchers,
            DomainEventPublisher domainEventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.projectionEventLog = projectionEventLog;
        this.channelDispatchers = List.copyOf(channelDispatchers);
        this.domainEventPublisher = domainEventPublisher;
    }

    public void onTodoItemCreated(TodoItemCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        Notification notification = Notification.create(
                UUID.randomUUID().toString(),
                buildDedupKey(event.eventType(), event.todoId()),
                event.tenantId(),
                event.assigneeId(),
                event.assigneeId(),
                null,
                event.title(),
                "Todo item requires attention",
                buildTodoDeepLink(event.todoId()),
                NotificationCategory.TODO_CREATED,
                NotificationPriority.fromUrgency(event.urgency()),
                SOURCE_MODULE,
                event.eventType(),
                event.todoId(),
                event.occurredAt()
        );

        createNotificationIfAbsent(notification);
    }

    public void onTodoItemOverdue(TodoItemOverdueEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        Notification notification = Notification.create(
                UUID.randomUUID().toString(),
                buildDedupKey(event.eventType(), event.todoId()),
                event.tenantId(),
                event.assigneeId(),
                event.assigneeId(),
                null,
                "Overdue: " + event.title(),
                "Todo item is overdue and requires immediate action",
                buildTodoDeepLink(event.todoId()),
                NotificationCategory.TODO_OVERDUE,
                NotificationPriority.CRITICAL,
                SOURCE_MODULE,
                event.eventType(),
                event.todoId(),
                event.occurredAt()
        );

        createNotificationIfAbsent(notification);
    }

    private void createNotificationIfAbsent(Notification notification) {
        if (notificationRepository.findByDedupKey(notification.dedupKey()).isPresent()) {
            return;
        }

        notificationRepository.save(notification);
        for (NotificationChannelDispatcher channelDispatcher : channelDispatchers) {
            NotificationDeliveryRecord deliveryRecord = channelDispatcher.dispatch(notification);
            deliveryRecordRepository.save(deliveryRecord);
            domainEventPublisher.publish(MsgNotificationSentEvent.from(notification, deliveryRecord));
            if (deliveryRecord.status() == NotificationDeliveryStatus.DELIVERED) {
                domainEventPublisher.publish(MsgNotificationDeliveredEvent.from(notification, deliveryRecord));
            }
        }
    }

    private static String buildDedupKey(String eventType, String todoId) {
        return eventType + ":" + todoId;
    }

    private static String buildTodoDeepLink(String todoId) {
        return "/portal/todo/" + todoId;
    }
}
