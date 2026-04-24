package com.hjo2oa.infra.timezone.interfaces;

import com.hjo2oa.infra.timezone.application.TimezoneSettingCommands;
import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TimezoneSettingDtos {

    private TimezoneSettingDtos() {
    }

    public record SetTimezoneRequest(@NotBlank @Size(max = 64) String timezoneId) {

        public TimezoneSettingCommands.SetSystemDefaultCommand toSystemCommand() {
            return new TimezoneSettingCommands.SetSystemDefaultCommand(timezoneId);
        }

        public TimezoneSettingCommands.SetTenantTimezoneCommand toTenantCommand(UUID tenantId) {
            return new TimezoneSettingCommands.SetTenantTimezoneCommand(tenantId, timezoneId);
        }

        public TimezoneSettingCommands.SetPersonTimezoneCommand toPersonCommand(UUID personId) {
            return new TimezoneSettingCommands.SetPersonTimezoneCommand(personId, timezoneId);
        }
    }

    public record ConvertToUtcRequest(
            @NotNull LocalDateTime localDateTime,
            @NotBlank @Size(max = 64) String timezoneId
    ) {

        public TimezoneSettingCommands.ConvertToUtcCommand toCommand() {
            return new TimezoneSettingCommands.ConvertToUtcCommand(localDateTime, timezoneId);
        }
    }

    public record ConvertFromUtcRequest(
            @NotNull Instant utcInstant,
            @NotBlank @Size(max = 64) String timezoneId
    ) {

        public TimezoneSettingCommands.ConvertFromUtcCommand toCommand() {
            return new TimezoneSettingCommands.ConvertFromUtcCommand(utcInstant, timezoneId);
        }
    }

    public record TimezoneSettingResponse(
            UUID id,
            TimezoneScopeType scopeType,
            UUID scopeId,
            String timezoneId,
            boolean isDefault,
            Instant effectiveFrom,
            boolean active,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ResolvedTimezoneResponse(
            UUID settingId,
            UUID tenantId,
            UUID personId,
            TimezoneScopeType scopeType,
            UUID scopeId,
            String timezoneId,
            boolean isDefault,
            Instant effectiveFrom
    ) {
    }

    public record ConvertToUtcResponse(Instant utcInstant, String timezoneId) {
    }

    public record ConvertFromUtcResponse(LocalDateTime localDateTime, String timezoneId) {
    }
}
