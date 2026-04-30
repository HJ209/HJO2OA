package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DomainEventEnvelopeFactory {

    private static final List<String> AGGREGATE_ID_FIELDS = List.of(
            "aggregateId",
            "entityId",
            "widgetId",
            "templateId",
            "publicationId",
            "profileId",
            "taskId",
            "todoId",
            "instanceId",
            "tenantId"
    );

    private static final List<String> ENVELOPE_FIELDS = List.of(
            "eventId",
            "eventType",
            "aggregateType",
            "aggregateId",
            "tenantId",
            "occurredAt",
            "traceId",
            "schemaVersion",
            "eventVersion",
            "correlationId"
    );

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DomainEventEnvelopeFactory(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    public DomainEventEnvelopeFactory(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DomainEventEnvelope from(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        UUID eventId = event.eventId() == null ? UUID.randomUUID() : event.eventId();
        Instant occurredAt = event.occurredAt() == null ? clock.instant() : event.occurredAt();
        String eventType = requireText(event.eventType(), "eventType");
        String tenantId = textOrDefault(event.tenantId(), "GLOBAL");
        JsonNode eventBody = objectMapper.valueToTree(event);
        return new DomainEventEnvelope(
                eventId,
                eventType,
                textOrDefault(reflectText(event, "aggregateType"), aggregateTypeFrom(eventType, event)),
                textOrDefault(reflectText(event, "aggregateId"), aggregateIdFrom(eventBody, eventId)),
                tenantId,
                occurredAt,
                textOrDefault(reflectText(event, "traceId"), textOrDefault(reflectText(event, "correlationId"),
                        eventId.toString())),
                textOrDefault(reflectText(event, "schemaVersion"), textOrDefault(reflectText(event, "eventVersion"),
                        "1")),
                payloadFrom(event, eventBody),
                event.getClass().getName(),
                eventBody
        );
    }

    private JsonNode payloadFrom(DomainEvent event, JsonNode eventBody) {
        Object explicitPayload = reflectValue(event, "payload");
        if (explicitPayload != null && explicitPayload != event) {
            return objectMapper.valueToTree(explicitPayload);
        }
        if (eventBody.isObject()) {
            ObjectNode payload = eventBody.deepCopy();
            payload.remove(ENVELOPE_FIELDS);
            return payload;
        }
        return eventBody;
    }

    private String aggregateTypeFrom(String eventType, DomainEvent event) {
        int lastDot = eventType.lastIndexOf('.');
        if (lastDot > 0) {
            return eventType.substring(0, lastDot);
        }
        return event.getClass().getSimpleName();
    }

    private String aggregateIdFrom(JsonNode eventBody, UUID eventId) {
        for (String fieldName : AGGREGATE_ID_FIELDS) {
            JsonNode value = eventBody.get(fieldName);
            if (isValueNode(value)) {
                return value.asText();
            }
        }
        if (eventBody.isObject()) {
            var fields = eventBody.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String fieldName = field.getKey();
                if (fieldName.endsWith("Id") && !"eventId".equals(fieldName)
                        && !"operatorAccountId".equals(fieldName) && !"operatorPersonId".equals(fieldName)
                        && isValueNode(field.getValue())) {
                    return field.getValue().asText();
                }
            }
        }
        return eventId.toString();
    }

    private boolean isValueNode(JsonNode value) {
        return value != null && !value.isNull() && value.isValueNode() && !value.asText().isBlank();
    }

    private String reflectText(Object target, String methodName) {
        Object value = reflectValue(target, methodName);
        return value == null ? null : String.valueOf(value);
    }

    private Object reflectValue(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0) {
                return null;
            }
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            return null;
        }
    }

    private String textOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
