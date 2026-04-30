package com.hjo2oa.msg.message.center.domain;

public enum NotificationInboxStatus {
    UNREAD,
    READ,
    ARCHIVED,
    REVOKED,
    EXPIRED,
    DELETED;

    public boolean isUnread() {
        return this == UNREAD;
    }

    public boolean isHiddenFromInbox() {
        return this == ARCHIVED || this == DELETED;
    }
}
