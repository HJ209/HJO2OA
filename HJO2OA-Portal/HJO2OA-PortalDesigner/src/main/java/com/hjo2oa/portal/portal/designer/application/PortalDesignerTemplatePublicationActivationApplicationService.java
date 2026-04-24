package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplatePublicationActivationApplicationService {

    private final PortalTemplateApplicationService templateApplicationService;
    private final PortalPublicationApplicationService publicationApplicationService;
    private final PortalDesignerTemplateStatusApplicationService templateStatusApplicationService;

    public PortalDesignerTemplatePublicationActivationApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            PortalDesignerTemplateStatusApplicationService templateStatusApplicationService
    ) {
        this.templateApplicationService = Objects.requireNonNull(
                templateApplicationService,
                "templateApplicationService must not be null"
        );
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.templateStatusApplicationService = Objects.requireNonNull(
                templateStatusApplicationService,
                "templateStatusApplicationService must not be null"
        );
    }

    public PortalDesignerTemplateStatusView activate(
            String templateId,
            String publicationId,
            PortalPublicationClientType clientType
    ) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(publicationId, "publicationId must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");

        PortalTemplateView template = templateApplicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        if (template.publishedVersionNo() == null) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Portal template must be published before activation"
            );
        }

        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                publicationId,
                template.templateId(),
                template.sceneType(),
                clientType,
                PortalPublicationAudience.tenantDefault()
        ));

        return templateStatusApplicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template status not found"
                ));
    }
}
