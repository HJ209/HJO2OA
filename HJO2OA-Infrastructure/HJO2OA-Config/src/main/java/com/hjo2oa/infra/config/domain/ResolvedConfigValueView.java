package com.hjo2oa.infra.config.domain;

import java.util.List;
import java.util.UUID;

public record ResolvedConfigValueView(
        UUID entryId,
        String configKey,
        ConfigType configType,
        ConfigStatus status,
        String resolvedValue,
        ResolvedValueSourceType sourceType,
        UUID overrideId,
        UUID featureRuleId,
        UUID tenantId,
        UUID orgId,
        UUID roleId,
        UUID userId,
        List<String> trace
) {

    public ResolvedConfigValueView {
        trace = trace == null ? List.of() : List.copyOf(trace);
    }
}
