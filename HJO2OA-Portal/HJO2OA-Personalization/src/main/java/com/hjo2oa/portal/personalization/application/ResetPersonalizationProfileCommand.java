package com.hjo2oa.portal.personalization.application;

import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;

public record ResetPersonalizationProfileCommand(
        PersonalizationSceneType sceneType,
        PersonalizationProfileScope scope,
        String assignmentId
) {
}
