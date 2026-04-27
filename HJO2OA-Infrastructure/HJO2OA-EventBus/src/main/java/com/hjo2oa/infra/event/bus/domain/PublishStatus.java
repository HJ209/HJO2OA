package com.hjo2oa.infra.event.bus.domain;

public enum PublishStatus {
    PENDING,
    PUBLISHED,
    PARTIALLY_DELIVERED,
    DELIVERED,
    DEAD_LETTERED
}
