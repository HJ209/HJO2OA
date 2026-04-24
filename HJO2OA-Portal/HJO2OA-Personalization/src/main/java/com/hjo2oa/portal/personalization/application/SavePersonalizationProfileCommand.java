package com.hjo2oa.portal.personalization.application;

import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntry;
import java.util.List;

public record SavePersonalizationProfileCommand(
        PersonalizationSceneType sceneType,
        PersonalizationProfileScope scope,
        String assignmentId,
        String themeCode,
        List<String> widgetOrderOverride,
        List<String> hiddenPlacementCodes,
        List<QuickAccessEntry> quickAccessEntries
) {
}
