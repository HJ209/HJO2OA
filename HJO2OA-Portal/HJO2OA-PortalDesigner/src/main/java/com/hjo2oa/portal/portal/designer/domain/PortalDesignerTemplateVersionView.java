package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalTemplateVersionStatus;
import java.time.Instant;

public record PortalDesignerTemplateVersionView(
        int versionNo,
        PortalTemplateVersionStatus status,
        Instant createdAt,
        Instant publishedAt,
        Instant deprecatedAt
) {
}
