package com.hjo2oa.portal.portal.home.infrastructure;

import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileView;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlay;
import com.hjo2oa.portal.portal.home.application.PortalHomePersonalizationOverlayProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PersonalizationPortalHomePersonalizationOverlayProvider implements PortalHomePersonalizationOverlayProvider {

    private final PersonalizationProfileApplicationService personalizationProfileApplicationService;

    public PersonalizationPortalHomePersonalizationOverlayProvider(
            PersonalizationProfileApplicationService personalizationProfileApplicationService
    ) {
        this.personalizationProfileApplicationService = Objects.requireNonNull(
                personalizationProfileApplicationService,
                "personalizationProfileApplicationService must not be null"
        );
    }

    @Override
    public PortalHomePersonalizationOverlay currentOverlay(PortalHomeSceneType sceneType) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        PersonalizationProfileView profile = personalizationProfileApplicationService.current(mapSceneType(sceneType));
        return new PortalHomePersonalizationOverlay(
                profile.basePublicationId(),
                profile.widgetOrderOverride(),
                profile.hiddenPlacementCodes()
        );
    }

    private PersonalizationSceneType mapSceneType(PortalHomeSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PersonalizationSceneType.HOME;
            case OFFICE_CENTER -> PersonalizationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PersonalizationSceneType.MOBILE_WORKBENCH;
        };
    }
}
