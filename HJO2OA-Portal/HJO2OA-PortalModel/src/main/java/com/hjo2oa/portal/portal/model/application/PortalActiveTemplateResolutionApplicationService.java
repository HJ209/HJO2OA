package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalActiveTemplateResolutionView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortalActiveTemplateResolutionApplicationService {

    private final PortalPublicationApplicationService publicationApplicationService;
    private final PortalTemplateApplicationService templateApplicationService;

    public PortalActiveTemplateResolutionApplicationService(
            PortalPublicationApplicationService publicationApplicationService,
            PortalTemplateApplicationService templateApplicationService
    ) {
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.templateApplicationService = Objects.requireNonNull(
                templateApplicationService,
                "templateApplicationService must not be null"
        );
    }

    public Optional<PortalActiveTemplateResolutionView> currentActive(
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType
    ) {
        return currentActive(sceneType, clientType, PortalPublicationIdentity.tenantDefault());
    }

    public Optional<PortalActiveTemplateResolutionView> currentActive(
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationIdentity identity
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");
        Objects.requireNonNull(identity, "identity must not be null");

        Optional<PortalPublicationView> publication = publicationApplicationService.currentActive(
                sceneType,
                clientType,
                identity
        );
        if (publication.isEmpty()) {
            return Optional.empty();
        }

        PortalPublicationView publicationView = publication.get();
        return templateApplicationService.current(publicationView.templateId())
                .map(template -> toView(publicationView, template));
    }

    private PortalActiveTemplateResolutionView toView(
            PortalPublicationView publication,
            PortalTemplateView template
    ) {
        return new PortalActiveTemplateResolutionView(
                publication.publicationId(),
                template.templateId(),
                template.templateCode(),
                template.displayName(),
                publication.sceneType(),
                publication.clientType(),
                publication.audience(),
                publication.status(),
                template.latestVersionNo(),
                template.publishedVersionNo(),
                publication.activatedAt(),
                publication.updatedAt(),
                template.updatedAt()
        );
    }
}
