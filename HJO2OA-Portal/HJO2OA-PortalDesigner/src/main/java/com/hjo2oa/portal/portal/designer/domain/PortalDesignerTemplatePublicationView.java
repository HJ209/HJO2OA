package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import java.time.Instant;

public record PortalDesignerTemplatePublicationView(
        String publicationId,
        String templateId,
        PortalPublicationSceneType sceneType,
        PortalPublicationClientType clientType,
        PortalPublicationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant offlinedAt
) {

    public static PortalDesignerTemplatePublicationView from(PortalPublicationView publication) {
        return new PortalDesignerTemplatePublicationView(
                publication.publicationId(),
                publication.templateId(),
                publication.sceneType(),
                publication.clientType(),
                publication.status(),
                publication.createdAt(),
                publication.updatedAt(),
                publication.activatedAt(),
                publication.offlinedAt()
        );
    }
}
