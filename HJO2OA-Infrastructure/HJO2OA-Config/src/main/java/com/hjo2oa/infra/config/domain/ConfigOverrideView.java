package com.hjo2oa.infra.config.domain;

import java.util.UUID;

public record ConfigOverrideView(
        UUID id,
        UUID configEntryId,
        OverrideScopeType scopeType,
        UUID scopeId,
        String overrideValue,
        boolean active
) {
}
