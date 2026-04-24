package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import java.util.Map;
import java.util.Objects;

public record SyncMappingRuleDraft(
        String sourceField,
        String targetField,
        Map<String, Object> transformRule,
        ConflictStrategy conflictStrategy,
        boolean keyMapping,
        int sortOrder
) {

    public SyncMappingRuleDraft {
        Objects.requireNonNull(conflictStrategy, "conflictStrategy must not be null");
        transformRule = transformRule == null ? Map.of() : Map.copyOf(transformRule);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
    }
}
