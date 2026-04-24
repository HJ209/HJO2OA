package com.hjo2oa.portal.personalization.infrastructure;

import com.hjo2oa.portal.personalization.domain.PersonalizationActivePublicationProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PortalModelActivePublicationProvider implements PersonalizationActivePublicationProvider {

    private final PortalPublicationApplicationService publicationApplicationService;
    private final PersonalizationIdentityContextProvider identityContextProvider;

    public PortalModelActivePublicationProvider(
            PortalPublicationApplicationService publicationApplicationService,
            PersonalizationIdentityContextProvider identityContextProvider
    ) {
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
    }

    @Override
    public Optional<String> findActivePublicationId(PersonalizationSceneType sceneType) {
        return findActivePublicationId(sceneType, identityContextProvider.currentContext());
    }

    @Override
    public Optional<String> findActivePublicationId(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(identityContext, "identityContext must not be null");
        PortalPublicationSceneType publicationSceneType = mapSceneType(sceneType);
        PortalPublicationIdentity identity = currentIdentity(identityContext);
        for (PortalPublicationClientType clientType : preferredClientTypes(sceneType)) {
            Optional<String> publicationId = publicationApplicationService.currentActive(
                    publicationSceneType,
                    clientType,
                    identity
            ).map(publication -> publication.publicationId());
            if (publicationId.isPresent()) {
                return publicationId;
            }
        }
        return Optional.empty();
    }

    private PortalPublicationIdentity currentIdentity(PersonalizationIdentityContext context) {
        return new PortalPublicationIdentity(
                context.assignmentId(),
                context.positionId(),
                context.personId()
        );
    }

    private PortalPublicationSceneType mapSceneType(PersonalizationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalPublicationSceneType.HOME;
            case OFFICE_CENTER -> PortalPublicationSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalPublicationSceneType.MOBILE_WORKBENCH;
        };
    }

    private List<PortalPublicationClientType> preferredClientTypes(PersonalizationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> List.of(PortalPublicationClientType.PC, PortalPublicationClientType.ALL);
            case OFFICE_CENTER -> List.of(PortalPublicationClientType.PC, PortalPublicationClientType.ALL);
            case MOBILE_WORKBENCH -> List.of(PortalPublicationClientType.MOBILE, PortalPublicationClientType.ALL);
        };
    }
}
