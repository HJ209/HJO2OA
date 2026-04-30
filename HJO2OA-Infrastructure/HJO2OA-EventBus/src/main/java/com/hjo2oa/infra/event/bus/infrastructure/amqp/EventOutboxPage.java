package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.util.List;

public record EventOutboxPage(
        List<EventOutboxEntity> items,
        long total
) {

    public EventOutboxPage {
        items = List.copyOf(items);
    }
}
