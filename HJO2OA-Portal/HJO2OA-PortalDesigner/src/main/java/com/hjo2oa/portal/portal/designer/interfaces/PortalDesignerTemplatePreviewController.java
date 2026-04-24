package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePreviewApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerPreviewIdentityContext;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePreviewView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/templates")
public class PortalDesignerTemplatePreviewController {

    private final PortalDesignerTemplatePreviewApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplatePreviewController(
            PortalDesignerTemplatePreviewApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{templateId}/preview")
    public ApiResponse<PortalDesignerTemplatePreviewView> preview(
            @PathVariable String templateId,
            @RequestParam(name = "clientType", required = false) PortalPublicationClientType clientType,
            @RequestParam(name = "tenantId", required = false) String tenantId,
            @RequestParam(name = "personId", required = false) String personId,
            @RequestParam(name = "accountId", required = false) String accountId,
            @RequestParam(name = "assignmentId", required = false) String assignmentId,
            @RequestParam(name = "positionId", required = false) String positionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.preview(
                        templateId,
                        clientType,
                        PortalDesignerPreviewIdentityContext.of(
                                tenantId,
                                personId,
                                accountId,
                                assignmentId,
                                positionId
                        )
                ),
                responseMetaFactory.create(request)
        );
    }
}
