package com.hjo2oa.infra.config.domain;

import java.util.Objects;
import java.util.UUID;

public record ConfigOverride(
        UUID id,
        UUID configEntryId,
        OverrideScopeType scopeType,
        UUID scopeId,
        String overrideValue,
        boolean active
) {

    public ConfigOverride {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(configEntryId, "configEntryId must not be null");
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(overrideValue, "overrideValue must not be null");
    }

    public static ConfigOverride active(
            UUID configEntryId,
            OverrideScopeType scopeType,
            UUID scopeId,
            String overrideValue
    ) {
        return new ConfigOverride(UUID.randomUUID(), configEntryId, scopeType, scopeId, overrideValue, true);
    }

    public ConfigOverride deactivate() {
        if (!active) {
            return this;
        }
        return new ConfigOverride(id, configEntryId, scopeType, scopeId, overrideValue, false);
    }

    public ConfigOverrideView toView() {
        return new ConfigOverrideView(id, configEntryId, scopeType, scopeId, overrideValue, active);
    }
}
