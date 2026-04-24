package com.hjo2oa.portal.personalization.domain;

public interface PersonalizationBasePublicationBindingManager {

    void bind(PersonalizationSceneType sceneType, String publicationId);

    void unbind(PersonalizationSceneType sceneType, String publicationId);

    void unbindAll(String publicationId);
}
