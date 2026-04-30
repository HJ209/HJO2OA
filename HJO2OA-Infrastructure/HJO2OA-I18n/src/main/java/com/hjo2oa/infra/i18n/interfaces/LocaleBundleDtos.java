package com.hjo2oa.infra.i18n.interfaces;

import com.hjo2oa.infra.i18n.application.LocaleBundleCommands;
import com.hjo2oa.infra.i18n.domain.LocaleBundleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LocaleBundleDtos {

    private LocaleBundleDtos() {
    }

    public record CreateBundleRequest(
            @NotBlank @Size(max = 64) String bundleCode,
            @NotBlank @Size(max = 64) String moduleCode,
            @NotBlank @Size(max = 16) String locale,
            @Size(max = 16) String fallbackLocale,
            UUID tenantId
    ) {

        public LocaleBundleCommands.CreateBundleCommand toCommand() {
            return new LocaleBundleCommands.CreateBundleCommand(
                    bundleCode,
                    moduleCode,
                    locale,
                    fallbackLocale,
                    tenantId
            );
        }
    }

    public record EntryRequest(
            @NotBlank @Size(max = 256) String resourceKey,
            @NotNull @Size(max = 4000) String resourceValue
    ) {

        public LocaleBundleCommands.BundleEntryCommand toCommand(UUID bundleId) {
            return new LocaleBundleCommands.BundleEntryCommand(bundleId, resourceKey, resourceValue);
        }
    }

    public record UpdateBundleRequest(
            @NotBlank @Size(max = 64) String moduleCode,
            @Size(max = 16) String fallbackLocale
    ) {

        public LocaleBundleCommands.UpdateBundleCommand toCommand() {
            return new LocaleBundleCommands.UpdateBundleCommand(moduleCode, fallbackLocale);
        }
    }

    public record UpdateEntryRequest(
            @NotNull @Size(max = 4000) String resourceValue
    ) {
    }

    public record ResolveMessageRequest(
            @NotBlank @Size(max = 64) String bundleCode,
            @NotBlank @Size(max = 256) String resourceKey,
            @NotBlank @Size(max = 16) String locale,
            UUID tenantId
    ) {

        public LocaleBundleCommands.ResolveMessageQuery toQuery() {
            return new LocaleBundleCommands.ResolveMessageQuery(bundleCode, resourceKey, locale, tenantId);
        }
    }

    public record EntryResponse(
            UUID id,
            UUID localeBundleId,
            String resourceKey,
            String resourceValue,
            int version,
            boolean active
    ) {
    }

    public record BundleResponse(
            UUID id,
            String bundleCode,
            String moduleCode,
            String locale,
            String fallbackLocale,
            LocaleBundleStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            List<EntryResponse> entries
    ) {
    }

    public record ResolvedMessageResponse(
            String bundleCode,
            String resourceKey,
            String requestedLocale,
            String resolvedLocale,
            String resourceValue,
            UUID tenantId,
            boolean usedFallback
    ) {
    }
}
