package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.Map;

public record SyncPayloadRecord(
        String recordKey,
        String checkpointToken,
        String eventId,
        Instant occurredAt,
        Map<String, Object> payload
) {

    public SyncPayloadRecord {
        recordKey = SyncDomainSupport.requireText(recordKey, "recordKey");
        checkpointToken = SyncDomainSupport.normalize(checkpointToken);
        eventId = SyncDomainSupport.normalize(eventId);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
