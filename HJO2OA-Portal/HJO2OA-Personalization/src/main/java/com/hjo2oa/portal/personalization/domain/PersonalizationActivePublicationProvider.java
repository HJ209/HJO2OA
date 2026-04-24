package com.hjo2oa.portal.personalization.domain;

import java.util.Optional;

public interface PersonalizationActivePublicationProvider {

    Optional<String> findActivePublicationId(PersonalizationSceneType sceneType);

    default Optional<String> findActivePublicationId(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext
    ) {
        return findActivePublicationId(sceneType);
    }
}
