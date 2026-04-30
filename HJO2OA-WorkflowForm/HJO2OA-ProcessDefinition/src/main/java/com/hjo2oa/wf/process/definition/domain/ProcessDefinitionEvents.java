package com.hjo2oa.wf.process.definition.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public final class ProcessDefinitionEvents {

    public static final String CREATED = "process.definition.created";
    public static final String PUBLISHED = "process.definition.published";
    public static final String DEPRECATED = "process.definition.deprecated";

    private ProcessDefinitionEvents() {
    }

    public record ProcessDefinitionChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID definitionId,
            String code,
            int version,
            String category,
            UUID formMetadataId
    ) implements DomainEvent {

        public static ProcessDefinitionChangedEvent of(String eventType, ProcessDefinition definition, Instant now) {
            return new ProcessDefinitionChangedEvent(
                    UUID.randomUUID(),
                    eventType,
                    now,
                    definition.tenantId().toString(),
                    definition.id(),
                    definition.code(),
                    definition.version(),
                    definition.category(),
                    definition.formMetadataId()
            );
        }
    }
}
