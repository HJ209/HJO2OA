package com.hjo2oa.infra.i18n.application;

import java.util.Objects;
import java.util.UUID;

public final class LocaleBundleCommands {

    private LocaleBundleCommands() {
    }

    public record CreateBundleCommand(
            String bundleCode,
            String moduleCode,
            String locale,
            String fallbackLocale,
            UUID tenantId
    ) {
    }

    public record BundleEntryCommand(
            UUID bundleId,
            String resourceKey,
            String resourceValue
    ) {

        public BundleEntryCommand {
            Objects.requireNonNull(bundleId, "bundleId must not be null");
        }
    }

    public record RemoveBundleEntryCommand(
            UUID bundleId,
            String resourceKey
    ) {

        public RemoveBundleEntryCommand {
            Objects.requireNonNull(bundleId, "bundleId must not be null");
        }
    }

    public record ResolveMessageQuery(
            String bundleCode,
            String resourceKey,
            String locale,
            UUID tenantId
    ) {
    }
}
