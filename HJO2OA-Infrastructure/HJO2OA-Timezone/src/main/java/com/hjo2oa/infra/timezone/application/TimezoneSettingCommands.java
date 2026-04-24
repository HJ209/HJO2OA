package com.hjo2oa.infra.timezone.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class TimezoneSettingCommands {

    private TimezoneSettingCommands() {
    }

    public record SetSystemDefaultCommand(String timezoneId) {

        public SetSystemDefaultCommand {
            timezoneId = requireText(timezoneId, "timezoneId");
        }
    }

    public record SetTenantTimezoneCommand(UUID tenantId, String timezoneId) {

        public SetTenantTimezoneCommand {
            Objects.requireNonNull(tenantId, "tenantId must not be null");
            timezoneId = requireText(timezoneId, "timezoneId");
        }
    }

    public record SetPersonTimezoneCommand(UUID personId, String timezoneId) {

        public SetPersonTimezoneCommand {
            Objects.requireNonNull(personId, "personId must not be null");
            timezoneId = requireText(timezoneId, "timezoneId");
        }
    }

    public record ResolveEffectiveTimezoneQuery(UUID tenantId, UUID personId) {
    }

    public record ConvertToUtcCommand(LocalDateTime localDateTime, String timezoneId) {

        public ConvertToUtcCommand {
            Objects.requireNonNull(localDateTime, "localDateTime must not be null");
            timezoneId = requireText(timezoneId, "timezoneId");
        }
    }

    public record ConvertFromUtcCommand(Instant utcInstant, String timezoneId) {

        public ConvertFromUtcCommand {
            Objects.requireNonNull(utcInstant, "utcInstant must not be null");
            timezoneId = requireText(timezoneId, "timezoneId");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
