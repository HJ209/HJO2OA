package com.hjo2oa.portal.personalization.domain;

public interface PersonalizationBasePublicationResolver {

    String resolveBasePublicationId(PersonalizationSceneType sceneType);

    default String resolveBasePublicationId(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext
    ) {
        return resolveBasePublicationId(sceneType);
    }
}
