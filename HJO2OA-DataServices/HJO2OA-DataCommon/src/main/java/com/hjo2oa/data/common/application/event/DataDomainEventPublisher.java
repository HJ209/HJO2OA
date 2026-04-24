package com.hjo2oa.data.common.application.event;

import com.hjo2oa.data.common.domain.event.DataDomainEvent;
import java.util.Collection;

public interface DataDomainEventPublisher {

    void publish(DataDomainEvent event);

    default void publishAll(Collection<? extends DataDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.stream()
                .filter(event -> event != null)
                .forEach(this::publish);
    }
}
