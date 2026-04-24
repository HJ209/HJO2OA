package com.hjo2oa.data.service.domain;

import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DataServiceActivatedEvent extends AbstractDataDomainEvent {

    public static final String EVENT_TYPE = "data.service.activated";
    public static final String MODULE_CODE = "data-service";
    public static final String AGGREGATE_CODE = "data-service-definition";

    private final UUID serviceId;
    private final String code;
    private final DataServiceDefinition.ServiceType serviceType;
    private final DataServiceDefinition.PermissionMode permissionMode;
    private final int statusSequence;
    private final Instant activatedAt;

    public DataServiceActivatedEvent(
            UUID eventId,
            Instant occurredAt,
            UUID serviceId,
            UUID tenantId,
            String code,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.PermissionMode permissionMode,
            int statusSequence,
            Instant activatedAt,
            String operatorId
    ) {
        super(
                eventId,
                EVENT_TYPE,
                occurredAt,
                tenantId.toString(),
                MODULE_CODE,
                AGGREGATE_CODE,
                operatorId,
                buildPayload(serviceId, code, serviceType, permissionMode, statusSequence, activatedAt)
        );
        this.serviceId = serviceId;
        this.code = code;
        this.serviceType = serviceType;
        this.permissionMode = permissionMode;
        this.statusSequence = statusSequence;
        this.activatedAt = activatedAt;
    }

    public static DataServiceActivatedEvent from(DataServiceDefinition definition) {
        return new DataServiceActivatedEvent(
                UUID.randomUUID(),
                definition.updatedAt(),
                definition.serviceId(),
                definition.tenantId(),
                definition.code(),
                definition.serviceType(),
                definition.permissionMode(),
                definition.statusSequence(),
                definition.activatedAt(),
                definition.updatedBy()
        );
    }

    public UUID serviceId() {
        return serviceId;
    }

    public String code() {
        return code;
    }

    public DataServiceDefinition.ServiceType serviceType() {
        return serviceType;
    }

    public DataServiceDefinition.PermissionMode permissionMode() {
        return permissionMode;
    }

    public int statusSequence() {
        return statusSequence;
    }

    public Instant activatedAt() {
        return activatedAt;
    }

    private static Map<String, Object> buildPayload(
            UUID serviceId,
            String code,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.PermissionMode permissionMode,
            int statusSequence,
            Instant activatedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("serviceId", serviceId);
        payload.put("code", code);
        payload.put("serviceType", serviceType.name());
        payload.put("permissionMode", permissionMode.name());
        payload.put("statusSequence", statusSequence);
        payload.put("activatedAt", activatedAt);
        return payload;
    }
}
