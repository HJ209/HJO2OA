package com.hjo2oa.msg.channel.sender.domain;

public enum DeliveryTaskStatus {
    PENDING,
    SENDING,
    DELIVERED,
    FAILED,
    GAVE_UP,
    CANCELLED
}
