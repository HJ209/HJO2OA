package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/templates")
public class PortalDesignerTemplateController {

    private final PortalDesignerTemplateStatusApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplateController(
            PortalDesignerTemplateStatusApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{templateId}/status")
    public ApiResponse<PortalDesignerTemplateStatusView> current(
            @PathVariable String templateId,
            HttpServletRequest request
    ) {
        PortalDesignerTemplateStatusView status = applicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template status not found"
                ));
        return ApiResponse.success(status, responseMetaFactory.create(request));
    }
}
