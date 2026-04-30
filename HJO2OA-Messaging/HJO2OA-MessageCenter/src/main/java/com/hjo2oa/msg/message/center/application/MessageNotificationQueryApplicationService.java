package com.hjo2oa.msg.message.center.application;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDetail;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MessageNotificationQueryApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRecordRepository deliveryRecordRepository;
    private final MessageIdentityContextProvider identityContextProvider;

    public MessageNotificationQueryApplicationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRecordRepository deliveryRecordRepository,
            MessageIdentityContextProvider identityContextProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.identityContextProvider = identityContextProvider;
    }

    public List<NotificationSummary> inbox() {
        return inbox(new MessageNotificationQuery(null, null, null, null));
    }

    public List<NotificationSummary> inbox(MessageNotificationQuery query) {
        MessageIdentityContext context = identityContextProvider.currentContext();
        MessageNotificationQuery resolvedQuery = query == null
                ? new MessageNotificationQuery(null, null, null, null)
                : query;
        List<NotificationCategory> categories = resolvedQuery.requestedCategories();
        return notificationRepository.findAll().stream()
                .filter(notification -> notification.isVisibleTo(context))
                .filter(notification -> matchesInboxStatus(notification, resolvedQuery))
                .filter(notification -> categories.isEmpty() || categories.contains(notification.category()))
                .filter(notification -> matchesText(notification.sourceModule(), resolvedQuery.sourceModule()))
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    public NotificationUnreadSummary unreadSummary() {
        List<NotificationSummary> inbox = inbox();
        List<NotificationSummary> unreadNotifications = inbox.stream()
                .filter(summary -> summary.inboxStatus().isUnread())
                .toList();

        Map<String, Long> unreadByCategory = unreadNotifications.stream()
                .collect(Collectors.groupingBy(
                        summary -> summary.category().name(),
                        Collectors.counting()
                ));

        List<String> latestNotificationIds = unreadNotifications.stream()
                .limit(5)
                .map(NotificationSummary::notificationId)
                .toList();

        return new NotificationUnreadSummary(
                unreadNotifications.size(),
                unreadByCategory,
                latestNotificationIds
        );
    }

    public Optional<NotificationSummary> summary(String notificationId) {
        MessageIdentityContext context = identityContextProvider.currentContext();
        return notificationRepository.findByNotificationId(notificationId)
                .filter(notification -> notification.isVisibleTo(context))
                .map(this::toSummary);
    }

    public Optional<NotificationDetail> detail(String notificationId) {
        MessageIdentityContext context = identityContextProvider.currentContext();
        return notificationRepository.findByNotificationId(notificationId)
                .filter(notification -> notification.isVisibleTo(context))
                .map(this::toDetail);
    }

    private NotificationSummary toSummary(Notification notification) {
        NotificationDeliveryStatus deliveryStatus = resolveDeliveryStatus(notification.notificationId());

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

    private NotificationDetail toDetail(Notification notification) {
        return new NotificationDetail(
                notification.notificationId(),
                notification.title(),
                notification.bodySummary(),
                notification.category(),
                notification.priority(),
                notification.inboxStatus(),
                resolveDeliveryStatus(notification.notificationId()),
                notification.sourceModule(),
                notification.sourceEventType(),
                notification.sourceBusinessId(),
                notification.deepLink(),
                notification.targetAssignmentId(),
                notification.targetPositionId(),
                notification.createdAt(),
                notification.readAt(),
                notification.archivedAt(),
                notification.revokedAt(),
                notification.expiredAt(),
                notification.statusReason()
        );
    }

    private NotificationDeliveryStatus resolveDeliveryStatus(String notificationId) {
        return deliveryRecordRepository.findByNotificationId(notificationId)
                .stream()
                .map(NotificationDeliveryRecord::status)
                .findFirst()
                .orElse(NotificationDeliveryStatus.PENDING);
    }

    private boolean matchesInboxStatus(Notification notification, MessageNotificationQuery query) {
        NotificationInboxStatus status = notification.inboxStatus();
        if (query.inboxStatus() != null) {
            return status == query.inboxStatus();
        }
        if (query.readStatus() != null && !query.readStatus().isBlank()
                && !"ALL".equalsIgnoreCase(query.readStatus())) {
            return status.name().equalsIgnoreCase(query.readStatus());
        }
        return !status.isHiddenFromInbox();
    }

    private boolean matchesText(String value, String expected) {
        return expected == null || expected.isBlank() || expected.equals(value);
    }
}
