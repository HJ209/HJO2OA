package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.home.application.PortalHomeOverlayApplicationResult;
import java.util.Objects;

public record PortalDesignerPreviewOverlayView(
        String status,
        String baselinePublicationId,
        String resolvedLivePublicationId,
        String reason
) {

    public PortalDesignerPreviewOverlayView {
        status = requireText(status, "status");
        reason = requireText(reason, "reason");
        baselinePublicationId = normalizeOptional(baselinePublicationId);
        resolvedLivePublicationId = normalizeOptional(resolvedLivePublicationId);
    }

    public static PortalDesignerPreviewOverlayView from(PortalHomeOverlayApplicationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PortalDesignerPreviewOverlayView(
                result.status(),
                result.baselinePublicationId(),
                result.resolvedPublicationId(),
                result.reason()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
