package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateInitializationView;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplateDraftApplicationService {

    private final PortalTemplateCanvasApplicationService templateCanvasApplicationService;
    private final PortalDesignerTemplateInitializationApplicationService initializationApplicationService;

    public PortalDesignerTemplateDraftApplicationService(
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            PortalDesignerTemplateInitializationApplicationService initializationApplicationService
    ) {
        this.templateCanvasApplicationService = Objects.requireNonNull(
                templateCanvasApplicationService,
                "templateCanvasApplicationService must not be null"
        );
        this.initializationApplicationService = Objects.requireNonNull(
                initializationApplicationService,
                "initializationApplicationService must not be null"
        );
    }

    public PortalDesignerTemplateInitializationView save(SavePortalTemplateCanvasCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        templateCanvasApplicationService.save(command);
        return initializationApplicationService.current(command.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template initialization not found"
                ));
    }
}
