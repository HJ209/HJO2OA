package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateDraftApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateInitializationView;
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
public class PortalDesignerTemplateDraftController {

    private final PortalDesignerTemplateDraftApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplateDraftController(
            PortalDesignerTemplateDraftApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PutMapping("/{templateId}/draft")
    public ApiResponse<PortalDesignerTemplateInitializationView> save(
            @PathVariable String templateId,
            @Valid @RequestBody SavePortalDesignerDraftRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.save(body.toCommand(templateId)),
                responseMetaFactory.create(request)
        );
    }
}
