package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;
import java.util.Objects;

public record PortalTemplateVersion(
        int versionNo,
        PortalTemplateVersionStatus status,
        Instant createdAt,
        Instant publishedAt,
        Instant deprecatedAt
) {

    public PortalTemplateVersion {
        if (versionNo <= 0) {
            throw new IllegalArgumentException("versionNo must be greater than 0");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (status == PortalTemplateVersionStatus.PUBLISHED) {
            Objects.requireNonNull(publishedAt, "publishedAt must not be null when status is PUBLISHED");
        }
        if (status == PortalTemplateVersionStatus.DEPRECATED) {
            Objects.requireNonNull(deprecatedAt, "deprecatedAt must not be null when status is DEPRECATED");
        }
    }

    public static PortalTemplateVersion draft(int versionNo, Instant now) {
        return new PortalTemplateVersion(versionNo, PortalTemplateVersionStatus.DRAFT, now, null, null);
    }

    public static PortalTemplateVersion published(int versionNo, Instant now) {
        return new PortalTemplateVersion(versionNo, PortalTemplateVersionStatus.PUBLISHED, now, now, null);
    }

    public PortalTemplateVersion publish(Instant now) {
        if (status == PortalTemplateVersionStatus.PUBLISHED) {
            return this;
        }
        if (status == PortalTemplateVersionStatus.DEPRECATED) {
            throw new IllegalStateException("Deprecated version cannot be published again");
        }
        return new PortalTemplateVersion(versionNo, PortalTemplateVersionStatus.PUBLISHED, createdAt, now, null);
    }

    public PortalTemplateVersion deprecate(Instant now) {
        if (status == PortalTemplateVersionStatus.DEPRECATED) {
            return this;
        }
        if (status == PortalTemplateVersionStatus.DRAFT) {
            throw new IllegalStateException("Draft version cannot be deprecated");
        }
        return new PortalTemplateVersion(versionNo, PortalTemplateVersionStatus.DEPRECATED, createdAt, publishedAt, now);
    }

    public PortalTemplateVersionView toView() {
        return new PortalTemplateVersionView(versionNo, status, createdAt, publishedAt, deprecatedAt);
    }
}
