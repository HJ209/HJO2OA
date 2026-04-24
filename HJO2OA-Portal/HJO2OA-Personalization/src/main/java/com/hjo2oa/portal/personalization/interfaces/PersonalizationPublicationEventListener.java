package com.hjo2oa.portal.personalization.interfaces;

import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationBindingManager;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PersonalizationPublicationEventListener {

    private final PersonalizationBasePublicationBindingManager bindingManager;

    public PersonalizationPublicationEventListener(
            PersonalizationBasePublicationBindingManager bindingManager
    ) {
        this.bindingManager = bindingManager;
    }

    @EventListener
    public void onPublicationActivated(PortalPublicationActivatedEvent event) {
        bindingManager.bind(mapSceneType(event.sceneType()), event.publicationId());
    }

    @EventListener
    public void onPublicationOfflined(PortalPublicationOfflinedEvent event) {
        PersonalizationSceneType sceneType = mapSceneType(event.sceneType());
        if (sceneType == null) {
            bindingManager.unbindAll(event.publicationId());
            return;
        }
        bindingManager.unbind(sceneType, event.publicationId());
    }

    private PersonalizationSceneType mapSceneType(PortalPublicationSceneType sceneType) {
        if (sceneType == null) {
            return null;
        }
        return switch (sceneType) {
            case HOME -> PersonalizationSceneType.HOME;
            case OFFICE_CENTER -> PersonalizationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PersonalizationSceneType.MOBILE_WORKBENCH;
        };
    }
}
