package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationChannelDispatcher;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InboxNotificationChannelDispatcher implements NotificationChannelDispatcher {

    @Override
    public NotificationDeliveryChannel channel() {
        return NotificationDeliveryChannel.INBOX;
    }

    @Override
    public NotificationDeliveryRecord dispatch(Notification notification) {
        return NotificationDeliveryRecord.pending(
                        UUID.randomUUID().toString(),
                        notification.notificationId(),
                        channel(),
                        notification.createdAt()
                )
                .markDelivered(notification.createdAt());
    }
}
