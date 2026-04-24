package com.hjo2oa.data.data.sync.domain;

import java.util.List;
import java.util.Map;

public record SyncPullBatch(
        List<SyncPayloadRecord> records,
        String nextCheckpoint,
        Map<String, Object> sourceContext
) {

    public SyncPullBatch {
        records = records == null ? List.of() : List.copyOf(records);
        nextCheckpoint = SyncDomainSupport.normalize(nextCheckpoint);
        sourceContext = sourceContext == null ? Map.of() : Map.copyOf(sourceContext);
    }
}
