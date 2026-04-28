package com.hjo2oa.portal.aggregation.api.application;

import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageListItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageListView;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageUnreadSummary;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.Pagination;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PortalMessageListAggregationApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MessageNotificationQueryApplicationService queryApplicationService;
    private final Clock clock;
    @Autowired
    public PortalMessageListAggregationApplicationService(
            MessageNotificationQueryApplicationService queryApplicationService
    ) {
        this(queryApplicationService, Clock.systemUTC());
    }
    public PortalMessageListAggregationApplicationService(
            MessageNotificationQueryApplicationService queryApplicationService,
            Clock clock
    ) {
        this.queryApplicationService = Objects.requireNonNull(
                queryApplicationService,
                "queryApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalMessageListView officeCenterMessages(
            int page,
            int size,
            NotificationCategory messageCategory,
            NotificationInboxStatus readStatus,
            String keyword
    ) {
        int normalizedPage = validatePage(page);
        int normalizedSize = validateSize(size);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<NotificationSummary> filteredNotifications = queryApplicationService.inbox().stream()
                .filter(summary -> matchesCategory(summary, messageCategory))
                .filter(summary -> matchesReadStatus(summary, readStatus))
                .filter(summary -> matchesKeyword(summary, normalizedKeyword))
                .toList();

        long total = filteredNotifications.size();
        int startIndex = Math.min((normalizedPage - 1) * normalizedSize, filteredNotifications.size());
        int endIndex = Math.min(startIndex + normalizedSize, filteredNotifications.size());

        List<PortalMessageListItem> items = filteredNotifications.subList(startIndex, endIndex).stream()
                .map(this::toPortalMessageListItem)
                .toList();

        return new PortalMessageListView(
                toPortalMessageUnreadSummary(queryApplicationService.unreadSummary()),
                new PageData<>(items, Pagination.of(normalizedPage, normalizedSize, total)),
                clock.instant()
        );
    }

    private int validatePage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size exceeds max page size " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private boolean matchesCategory(NotificationSummary summary, NotificationCategory messageCategory) {
        return messageCategory == null || summary.category() == messageCategory;
    }

    private boolean matchesReadStatus(NotificationSummary summary, NotificationInboxStatus readStatus) {
        return readStatus == null || summary.inboxStatus() == readStatus;
    }

    private boolean matchesKeyword(NotificationSummary summary, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsIgnoreCase(summary.title(), keyword) || containsIgnoreCase(summary.bodySummary(), keyword);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
    }

    private PortalMessageListItem toPortalMessageListItem(NotificationSummary summary) {
        return new PortalMessageListItem(
                summary.notificationId(),
                summary.title(),
                summary.bodySummary(),
                summary.category().name(),
                summary.priority().name(),
                summary.inboxStatus().name(),
                summary.deliveryStatus().name(),
                summary.sourceModule(),
                summary.deepLink(),
                summary.targetAssignmentId(),
                summary.targetPositionId(),
                summary.createdAt()
        );
    }

    private PortalMessageUnreadSummary toPortalMessageUnreadSummary(NotificationUnreadSummary unreadSummary) {
        if (unreadSummary == null) {
            return PortalMessageUnreadSummary.empty();
        }
        return new PortalMessageUnreadSummary(
                unreadSummary.totalUnreadCount(),
                unreadSummary.categoryUnreadCounts(),
                unreadSummary.latestNotificationIds()
        );
    }
}
