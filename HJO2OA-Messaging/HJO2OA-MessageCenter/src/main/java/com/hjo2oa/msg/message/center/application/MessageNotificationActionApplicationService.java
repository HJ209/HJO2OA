package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationAction;
import com.hjo2oa.msg.message.center.domain.NotificationActionRepository;
import com.hjo2oa.msg.message.center.domain.NotificationBulkReadResult;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MessageNotificationActionApplicationService {

    private static final int MAX_BULK_READ_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationActionRepository notificationActionRepository;
    private final MessageIdentityContextProvider identityContextProvider;
    private final MessageNotificationQueryApplicationService queryApplicationService;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public MessageNotificationActionApplicationService(
            NotificationRepository notificationRepository,
            NotificationActionRepository notificationActionRepository,
            MessageIdentityContextProvider identityContextProvider,
            MessageNotificationQueryApplicationService queryApplicationService,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                notificationRepository,
                notificationActionRepository,
                identityContextProvider,
                queryApplicationService,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public MessageNotificationActionApplicationService(
            NotificationRepository notificationRepository,
            NotificationActionRepository notificationActionRepository,
            MessageIdentityContextProvider identityContextProvider,
            MessageNotificationQueryApplicationService queryApplicationService,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
        this.notificationActionRepository = Objects.requireNonNull(notificationActionRepository, "notificationActionRepository must not be null");
        this.identityContextProvider = Objects.requireNonNull(identityContextProvider, "identityContextProvider must not be null");
        this.queryApplicationService = Objects.requireNonNull(queryApplicationService, "queryApplicationService must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Optional<NotificationSummary> markRead(String notificationId) {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        MessageIdentityContext context = identityContextProvider.currentContext();
        Optional<Notification> foundNotification = visibleNotification(notificationId, context);
        if (foundNotification.isEmpty()) {
            return Optional.empty();
        }

        Notification notification = foundNotification.orElseThrow();
        if (notification.isUnread()) {
            notification = persistRead(notification, context);
        }

        return queryApplicationService.summary(notification.notificationId());
    }

    public NotificationBulkReadResult bulkMarkRead(List<String> notificationIds) {
        List<String> normalizedIds = normalizeNotificationIds(notificationIds);
        MessageIdentityContext context = identityContextProvider.currentContext();

        List<NotificationSummary> notifications = new ArrayList<>();
        List<String> missingNotificationIds = new ArrayList<>();
        int readCount = 0;
        int alreadyReadCount = 0;

        for (String notificationId : normalizedIds) {
            Optional<Notification> foundNotification = visibleNotification(notificationId, context);
            if (foundNotification.isEmpty()) {
                missingNotificationIds.add(notificationId);
                continue;
            }

            Notification notification = foundNotification.orElseThrow();
            if (notification.isUnread()) {
                notification = persistRead(notification, context);
                readCount++;
            } else {
                alreadyReadCount++;
            }

            notifications.add(queryApplicationService.summary(notification.notificationId()).orElseThrow());
        }

        return new NotificationBulkReadResult(
                normalizedIds.size(),
                notifications.size(),
                readCount,
                alreadyReadCount,
                missingNotificationIds.size(),
                notifications,
                missingNotificationIds
        );
    }

    private Instant now() {
        return clock.instant();
    }

    private Optional<Notification> visibleNotification(String notificationId, MessageIdentityContext context) {
        return notificationRepository.findByNotificationId(notificationId)
                .filter(notification -> notification.isVisibleTo(context));
    }

    private Notification persistRead(Notification notification, MessageIdentityContext context) {
        Notification readNotification = notification.markRead(now());
        notificationRepository.save(readNotification);
        notificationActionRepository.save(NotificationAction.read(
                UUID.randomUUID().toString(),
                readNotification.notificationId(),
                context.recipientId(),
                readNotification.readAt()
        ));
        domainEventPublisher.publish(MsgNotificationReadEvent.from(readNotification));
        return readNotification;
    }

    private List<String> normalizeNotificationIds(List<String> notificationIds) {
        Objects.requireNonNull(notificationIds, "notificationIds must not be null");
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        for (String notificationId : notificationIds) {
            Objects.requireNonNull(notificationId, "notificationId must not be null");
            String normalizedId = notificationId.trim();
            if (normalizedId.isEmpty()) {
                throw new IllegalArgumentException("notificationId must not be blank");
            }
            normalizedIds.add(normalizedId);
        }
        if (normalizedIds.isEmpty()) {
            throw new IllegalArgumentException("notificationIds must not be empty");
        }
        if (normalizedIds.size() > MAX_BULK_READ_SIZE) {
            throw new IllegalArgumentException("notificationIds exceeds max bulk size " + MAX_BULK_READ_SIZE);
        }
        return List.copyOf(normalizedIds);
    }
}
