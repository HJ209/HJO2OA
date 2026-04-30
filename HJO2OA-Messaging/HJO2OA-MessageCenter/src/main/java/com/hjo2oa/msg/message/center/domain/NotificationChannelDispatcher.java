package com.hjo2oa.msg.message.center.domain;

import java.util.List;

public interface NotificationChannelDispatcher {

    NotificationDeliveryChannel channel();

    List<NotificationDeliveryRecord> dispatch(Notification notification);
}
