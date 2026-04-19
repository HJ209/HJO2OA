package com.hjo2oa.msg.message.center.domain;

public interface NotificationChannelDispatcher {

    NotificationDeliveryChannel channel();

    NotificationDeliveryRecord dispatch(Notification notification);
}
