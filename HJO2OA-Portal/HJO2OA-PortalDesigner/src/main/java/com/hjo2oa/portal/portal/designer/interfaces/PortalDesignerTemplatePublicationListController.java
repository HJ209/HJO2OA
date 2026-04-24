package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublicationListApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/templates")
public class PortalDesignerTemplatePublicationListController {

    private final PortalDesignerTemplatePublicationListApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplatePublicationListController(
            PortalDesignerTemplatePublicationListApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{templateId}/publications")
    public ApiResponse<List<PortalDesignerTemplatePublicationView>> list(
            @PathVariable String templateId,
            @RequestParam(required = false) PortalPublicationClientType clientType,
            @RequestParam(required = false) PortalPublicationStatus status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.list(templateId, clientType, status),
                responseMetaFactory.create(request)
        );
    }
}
