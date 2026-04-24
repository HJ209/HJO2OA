package com.hjo2oa.portal.portal.model.application;

import java.util.Objects;

public record OfflinePortalPublicationCommand(String publicationId) {

    public OfflinePortalPublicationCommand {
        publicationId = requireText(publicationId, "publicationId");
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
