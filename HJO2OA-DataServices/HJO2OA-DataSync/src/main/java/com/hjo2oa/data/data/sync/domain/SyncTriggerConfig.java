package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncTriggerConfig(
        boolean manualTriggerEnabled,
        List<String> eventPatterns,
        String schedulerJobCode
) {

    public SyncTriggerConfig {
        eventPatterns = eventPatterns == null ? List.of() : List.copyOf(eventPatterns);
        schedulerJobCode = normalize(schedulerJobCode);
    }

    public static SyncTriggerConfig manualOnly() {
        return new SyncTriggerConfig(true, List.of(), null);
    }

    public boolean matchesEvent(String eventType) {
        if (eventPatterns.isEmpty() || eventType == null || eventType.isBlank()) {
            return false;
        }
        return eventPatterns.stream().anyMatch(pattern -> wildcardMatch(pattern, eventType));
    }

    private static boolean wildcardMatch(String pattern, String value) {
        String normalizedPattern = normalize(pattern);
        if (normalizedPattern == null) {
            return false;
        }
        if (!normalizedPattern.contains("*")) {
            return normalizedPattern.equals(value);
        }
        String prefix = normalizedPattern.substring(0, normalizedPattern.indexOf('*'));
        String suffix = normalizedPattern.substring(normalizedPattern.indexOf('*') + 1);
        return value.startsWith(prefix) && value.endsWith(suffix);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
