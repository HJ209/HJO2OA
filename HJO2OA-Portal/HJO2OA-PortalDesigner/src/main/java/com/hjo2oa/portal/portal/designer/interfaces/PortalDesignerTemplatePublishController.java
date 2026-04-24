package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublishApplicationService;
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
public class PortalDesignerTemplatePublishController {

    private final PortalDesignerTemplatePublishApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplatePublishController(
            PortalDesignerTemplatePublishApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PutMapping("/{templateId}/publish")
    public ApiResponse<PortalDesignerTemplateStatusView> publish(
            @PathVariable String templateId,
            @Valid @RequestBody PublishPortalDesignerTemplateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.publish(body.toCommand(templateId)),
                responseMetaFactory.create(request)
        );
    }
}
