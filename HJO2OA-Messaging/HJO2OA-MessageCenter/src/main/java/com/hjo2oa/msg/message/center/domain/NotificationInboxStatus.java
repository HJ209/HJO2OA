package com.hjo2oa.msg.message.center.domain;

public enum NotificationInboxStatus {
    UNREAD,
    READ,
    ARCHIVED,
    REVOKED,
    EXPIRED;

    public boolean isUnread() {
        return this == UNREAD;
    }
}
