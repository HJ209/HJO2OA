package com.hjo2oa.portal.personalization.domain;

import java.time.Instant;
import java.util.List;

public record PersonalizationProfileView(
        String profileId,
        String tenantId,
        String personId,
        String assignmentId,
        PersonalizationSceneType sceneType,
        PersonalizationProfileScope resolvedScope,
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

    public PersonalizationProfileView {
        widgetOrderOverride = widgetOrderOverride == null ? List.of() : List.copyOf(widgetOrderOverride);
        hiddenPlacementCodes = hiddenPlacementCodes == null ? List.of() : List.copyOf(hiddenPlacementCodes);
        quickAccessEntries = quickAccessEntries == null ? List.of() : List.copyOf(quickAccessEntries);
    }
}
