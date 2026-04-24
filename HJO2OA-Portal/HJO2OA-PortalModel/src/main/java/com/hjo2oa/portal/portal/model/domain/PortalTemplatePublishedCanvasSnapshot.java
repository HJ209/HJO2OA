package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PortalTemplatePublishedCanvasSnapshot(
        int versionNo,
        Instant capturedAt,
        List<PortalPage> pages
) {

    public PortalTemplatePublishedCanvasSnapshot {
        if (versionNo <= 0) {
            throw new IllegalArgumentException("versionNo must be greater than 0");
        }
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        pages = PortalTemplate.copyPages(pages);
    }

    public static PortalTemplatePublishedCanvasSnapshot capture(
            int versionNo,
            List<PortalPage> pages,
            Instant capturedAt
    ) {
        return new PortalTemplatePublishedCanvasSnapshot(versionNo, capturedAt, pages);
    }
}
