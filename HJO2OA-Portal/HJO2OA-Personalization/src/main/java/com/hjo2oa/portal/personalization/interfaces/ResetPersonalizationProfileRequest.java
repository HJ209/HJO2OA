package com.hjo2oa.portal.personalization.interfaces;

import com.hjo2oa.portal.personalization.application.ResetPersonalizationProfileCommand;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import jakarta.validation.constraints.NotNull;

public record ResetPersonalizationProfileRequest(
        @NotNull PersonalizationSceneType sceneType,
        PersonalizationProfileScope scope,
        String assignmentId
) {

    public ResetPersonalizationProfileCommand toCommand() {
        return new ResetPersonalizationProfileCommand(sceneType, scope, assignmentId);
    }
}
