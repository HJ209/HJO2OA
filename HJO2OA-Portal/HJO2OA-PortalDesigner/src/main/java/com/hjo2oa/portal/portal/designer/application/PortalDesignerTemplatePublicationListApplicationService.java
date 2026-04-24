package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplatePublicationListApplicationService {

    private final PortalDesignerTemplateProjectionRepository projectionRepository;
    private final PortalPublicationApplicationService publicationApplicationService;
    private final PortalModelContextProvider contextProvider;

    public PortalDesignerTemplatePublicationListApplicationService(
            PortalDesignerTemplateProjectionRepository projectionRepository,
            PortalPublicationApplicationService publicationApplicationService,
            PortalModelContextProvider contextProvider
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
    }

    public List<PortalDesignerTemplatePublicationView> list(
            String templateId,
            PortalPublicationClientType clientType,
            PortalPublicationStatus status
    ) {
        Objects.requireNonNull(templateId, "templateId must not be null");

        PortalDesignerTemplateProjection projection = currentTenantTemplate(templateId);
        return publicationApplicationService.list(projection.sceneType(), clientType, status).stream()
                .filter(publication -> publication.templateId().equals(templateId))
                .map(PortalDesignerTemplatePublicationView::from)
                .sorted(Comparator.comparing(PortalDesignerTemplatePublicationView::publicationId))
                .toList();
    }

    private PortalDesignerTemplateProjection currentTenantTemplate(String templateId) {
        PortalModelContext context = contextProvider.currentContext();
        return projectionRepository.findAllByTenant(context.tenantId()).stream()
                .filter(projection -> projection.templateId().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template not found"
                ));
    }
}
