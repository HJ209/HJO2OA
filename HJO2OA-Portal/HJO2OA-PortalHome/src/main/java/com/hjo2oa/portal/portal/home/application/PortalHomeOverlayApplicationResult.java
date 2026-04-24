package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import java.util.Objects;

public record PortalHomeOverlayApplicationResult(
        PortalHomePageTemplate template,
        boolean applied,
        String baselinePublicationId,
        String resolvedPublicationId,
        String reason
) {

    public PortalHomeOverlayApplicationResult {
        Objects.requireNonNull(template, "template must not be null");
        reason = requireText(reason, "reason");
        baselinePublicationId = normalizeOptional(baselinePublicationId);
        resolvedPublicationId = normalizeOptional(resolvedPublicationId);
    }

    public static PortalHomeOverlayApplicationResult applied(
            PortalHomePageTemplate template,
            String baselinePublicationId,
            String resolvedPublicationId,
            String reason
    ) {
        return new PortalHomeOverlayApplicationResult(
                template,
                true,
                baselinePublicationId,
                resolvedPublicationId,
                reason
        );
    }

    public static PortalHomeOverlayApplicationResult bypassed(
            PortalHomePageTemplate template,
            String baselinePublicationId,
            String resolvedPublicationId,
            String reason
    ) {
        return new PortalHomeOverlayApplicationResult(
                template,
                false,
                baselinePublicationId,
                resolvedPublicationId,
                reason
        );
    }

    public String status() {
        return applied ? "applied" : "bypassed";
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
