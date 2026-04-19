package com.hjo2oa.msg.message.center.domain;

import java.util.UUID;

public interface NotificationProjectionEventLog {

    boolean registerIfAbsent(UUID eventId);
}
