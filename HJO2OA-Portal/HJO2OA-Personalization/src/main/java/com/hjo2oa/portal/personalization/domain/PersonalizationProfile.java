package com.hjo2oa.portal.personalization.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PersonalizationProfile(
        String profileId,
        String tenantId,
        String personId,
        String assignmentId,
        PersonalizationSceneType sceneType,
        String basePublicationId,
        String themeCode,
        List<String> widgetOrderOverride,
        List<String> hiddenPlacementCodes,
        List<QuickAccessEntry> quickAccessEntries,
        PersonalizationProfileStatus status,
        Instant lastResolvedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public PersonalizationProfile {
        profileId = requireText(profileId, "profileId");
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        assignmentId = normalizeOptional(assignmentId);
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        basePublicationId = requireText(basePublicationId, "basePublicationId");
        themeCode = normalizeOptional(themeCode);
        widgetOrderOverride = List.copyOf(Objects.requireNonNull(widgetOrderOverride, "widgetOrderOverride must not be null"));
        hiddenPlacementCodes = List.copyOf(
                Objects.requireNonNull(hiddenPlacementCodes, "hiddenPlacementCodes must not be null")
        );
        quickAccessEntries = List.copyOf(Objects.requireNonNull(quickAccessEntries, "quickAccessEntries must not be null"));
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(lastResolvedAt, "lastResolvedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PersonalizationProfile create(
            String profileId,
            String tenantId,
            String personId,
            String assignmentId,
            PersonalizationSceneType sceneType,
            String basePublicationId,
            Instant now
    ) {
        return new PersonalizationProfile(
                profileId,
                tenantId,
                personId,
                assignmentId,
                sceneType,
                basePublicationId,
                null,
                List.of(),
                List.of(),
                List.of(),
                PersonalizationProfileStatus.RESET,
                now,
                now,
                now
        );
    }

    public PersonalizationProfile saveOverrides(
            String themeCode,
            List<String> widgetOrderOverride,
            List<String> hiddenPlacementCodes,
            List<QuickAccessEntry> quickAccessEntries,
            Instant now
    ) {
        return new PersonalizationProfile(
                profileId,
                tenantId,
                personId,
                assignmentId,
                sceneType,
                basePublicationId,
                themeCode,
                widgetOrderOverride,
                hiddenPlacementCodes,
                quickAccessEntries,
                PersonalizationProfileStatus.ACTIVE,
                now,
                createdAt,
                now
        );
    }

    public PersonalizationProfile reset(String resolvedBasePublicationId, Instant now) {
        return new PersonalizationProfile(
                profileId,
                tenantId,
                personId,
                assignmentId,
                sceneType,
                requireText(resolvedBasePublicationId, "resolvedBasePublicationId"),
                null,
                List.of(),
                List.of(),
                List.of(),
                PersonalizationProfileStatus.RESET,
                now,
                createdAt,
                now
        );
    }

    public PersonalizationProfileKey key() {
        return assignmentId == null
                ? PersonalizationProfileKey.ofGlobal(tenantId, personId, sceneType)
                : PersonalizationProfileKey.ofAssignment(tenantId, personId, assignmentId, sceneType);
    }

    public PersonalizationProfileScope scope() {
        return assignmentId == null ? PersonalizationProfileScope.GLOBAL : PersonalizationProfileScope.ASSIGNMENT;
    }

    public PersonalizationProfileView toView() {
        return new PersonalizationProfileView(
                profileId,
                tenantId,
                personId,
                assignmentId,
                sceneType,
                scope(),
                basePublicationId,
                themeCode,
                widgetOrderOverride,
                hiddenPlacementCodes,
                quickAccessEntries,
                status,
                lastResolvedAt,
                createdAt,
                updatedAt
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
