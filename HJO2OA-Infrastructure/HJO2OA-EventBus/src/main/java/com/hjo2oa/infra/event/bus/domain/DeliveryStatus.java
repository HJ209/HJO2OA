package com.hjo2oa.infra.event.bus.domain;

public enum DeliveryStatus {
    SUCCESS,
    FAILED,
    RETRYING,
    DEAD_LETTERED
}
