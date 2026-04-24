package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplatePublishApplicationService {

    private final PortalTemplateApplicationService templateApplicationService;
    private final PortalDesignerTemplateStatusApplicationService templateStatusApplicationService;

    public PortalDesignerTemplatePublishApplicationService(
            PortalTemplateApplicationService templateApplicationService,
            PortalDesignerTemplateStatusApplicationService templateStatusApplicationService
    ) {
        this.templateApplicationService = Objects.requireNonNull(
                templateApplicationService,
                "templateApplicationService must not be null"
        );
        this.templateStatusApplicationService = Objects.requireNonNull(
                templateStatusApplicationService,
                "templateStatusApplicationService must not be null"
        );
    }

    public PortalDesignerTemplateStatusView publish(PublishPortalTemplateVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        templateApplicationService.publish(command);
        return templateStatusApplicationService.current(command.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template status not found"
                ));
    }
}
