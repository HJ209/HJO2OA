package com.hjo2oa.portal.personalization.application;

import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.ValidatedPersonalizationOverlay;
import java.util.List;

public interface PersonalizationOverlaySaveValidator {

    ValidatedPersonalizationOverlay validate(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext,
            String resolvedBasePublicationId,
            String existingBasePublicationId,
            List<String> widgetOrderOverride,
            List<String> hiddenPlacementCodes
    );

    static PersonalizationOverlaySaveValidator noop() {
        return (sceneType,
                identityContext,
                resolvedBasePublicationId,
                existingBasePublicationId,
                widgetOrderOverride,
                hiddenPlacementCodes) -> new ValidatedPersonalizationOverlay(
                resolvedBasePublicationId,
                widgetOrderOverride,
                hiddenPlacementCodes
        );
    }
}
