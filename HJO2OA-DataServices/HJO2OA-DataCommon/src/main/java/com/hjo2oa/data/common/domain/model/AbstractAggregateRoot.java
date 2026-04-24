package com.hjo2oa.data.common.domain.model;

import com.hjo2oa.data.common.domain.event.DataDomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractAggregateRoot extends AbstractDomainEntity {

    private final List<DataDomainEvent> domainEvents = new ArrayList<>();

    protected AbstractAggregateRoot(UUID id) {
        super(id);
    }

    protected void registerEvent(DataDomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    public List<DataDomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public List<DataDomainEvent> pullDomainEvents() {
        List<DataDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
