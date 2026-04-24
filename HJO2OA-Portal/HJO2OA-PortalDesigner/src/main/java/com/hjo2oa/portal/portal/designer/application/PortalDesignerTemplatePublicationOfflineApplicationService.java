package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplatePublicationOfflineApplicationService {

    private final PortalPublicationApplicationService publicationApplicationService;
    private final PortalDesignerTemplateStatusApplicationService templateStatusApplicationService;

    public PortalDesignerTemplatePublicationOfflineApplicationService(
            PortalPublicationApplicationService publicationApplicationService,
            PortalDesignerTemplateStatusApplicationService templateStatusApplicationService
    ) {
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.templateStatusApplicationService = Objects.requireNonNull(
                templateStatusApplicationService,
                "templateStatusApplicationService must not be null"
        );
    }

    public PortalDesignerTemplateStatusView offline(String templateId, String publicationId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(publicationId, "publicationId must not be null");

        PortalPublicationView publication = publicationApplicationService.current(publicationId)
                .filter(current -> current.templateId().equals(templateId))
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal publication not found for template"
                ));

        publicationApplicationService.offline(new OfflinePortalPublicationCommand(publication.publicationId()));

        return templateStatusApplicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template status not found"
                ));
    }
}
