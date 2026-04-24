package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublicationOfflineApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/templates")
public class PortalDesignerTemplatePublicationOfflineController {

    private final PortalDesignerTemplatePublicationOfflineApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplatePublicationOfflineController(
            PortalDesignerTemplatePublicationOfflineApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/{templateId}/publications/{publicationId}/offline")
    public ApiResponse<PortalDesignerTemplateStatusView> offline(
            @PathVariable String templateId,
            @PathVariable String publicationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.offline(templateId, publicationId),
                responseMetaFactory.create(request)
        );
    }
}
