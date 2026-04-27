package com.hjo2oa.infra.event.bus.domain;

import java.util.Optional;
import java.util.UUID;

public interface EventDefinitionRepository {

    EventDefinition save(EventDefinition definition);

    Optional<EventDefinition> findById(UUID id);

    Optional<EventDefinition> findByEventTypeAndVersion(String eventType, String version);

    java.util.List<EventDefinition> findByModulePrefix(String modulePrefix);

    java.util.List<EventDefinition> findAll();
}
