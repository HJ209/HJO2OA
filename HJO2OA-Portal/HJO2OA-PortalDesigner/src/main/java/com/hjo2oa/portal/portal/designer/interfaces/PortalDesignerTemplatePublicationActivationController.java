package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublicationActivationApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/templates")
public class PortalDesignerTemplatePublicationActivationController {

    private final PortalDesignerTemplatePublicationActivationApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplatePublicationActivationController(
            PortalDesignerTemplatePublicationActivationApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PutMapping("/{templateId}/publications/{publicationId}/activate")
    public ApiResponse<PortalDesignerTemplateStatusView> activate(
            @PathVariable String templateId,
            @PathVariable String publicationId,
            @Valid @RequestBody ActivatePortalDesignerPublicationRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.activate(templateId, publicationId, body.clientType()),
                responseMetaFactory.create(request)
        );
    }
}
