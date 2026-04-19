package com.hjo2oa.portal.aggregation.api.infrastructure;

import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MessageCenterPortalMessageCardDataProvider implements PortalMessageCardDataProvider {

    private static final int DEFAULT_TOP_ITEM_LIMIT = 5;

    private final MessageNotificationQueryApplicationService queryApplicationService;

    public MessageCenterPortalMessageCardDataProvider(MessageNotificationQueryApplicationService queryApplicationService) {
        this.queryApplicationService = queryApplicationService;
    }

    @Override
    public PortalMessageCard currentMessageCard() {
        NotificationUnreadSummary unreadSummary = queryApplicationService.unreadSummary();
        List<PortalMessageItem> topItems = queryApplicationService.inbox().stream()
                .limit(DEFAULT_TOP_ITEM_LIMIT)
                .map(this::toPortalMessageItem)
                .toList();

        return new PortalMessageCard(
                unreadSummary.totalUnreadCount(),
                unreadSummary.categoryUnreadCounts(),
                topItems
        );
    }

    private PortalMessageItem toPortalMessageItem(NotificationSummary summary) {
        return new PortalMessageItem(
                summary.notificationId(),
                summary.title(),
                summary.category().name(),
                summary.priority().name(),
                summary.deepLink(),
                summary.createdAt()
        );
    }
}
