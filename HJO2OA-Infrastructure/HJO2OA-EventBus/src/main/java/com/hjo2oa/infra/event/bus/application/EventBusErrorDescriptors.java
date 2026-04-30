package com.hjo2oa.infra.event.bus.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import org.springframework.http.HttpStatus;

public final class EventBusErrorDescriptors {

    public static final ErrorDescriptor EVENT_NOT_FOUND =
            new ErrorDescriptor("EVENT_BUS_EVENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Event outbox record not found");

    public static final ErrorDescriptor INVALID_OPERATION =
            new ErrorDescriptor("EVENT_BUS_INVALID_OPERATION", HttpStatus.UNPROCESSABLE_ENTITY,
                    "EventBus operation is not allowed");

    private EventBusErrorDescriptors() {
    }
}
