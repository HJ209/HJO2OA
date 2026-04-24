package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;

public record PortalTemplateVersionView(
        int versionNo,
        PortalTemplateVersionStatus status,
        Instant createdAt,
        Instant publishedAt,
        Instant deprecatedAt
) {
}
