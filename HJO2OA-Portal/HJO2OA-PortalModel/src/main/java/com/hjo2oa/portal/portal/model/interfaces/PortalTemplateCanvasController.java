package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/model/templates")
public class PortalTemplateCanvasController {

    private final PortalTemplateCanvasApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalTemplateCanvasController(
            PortalTemplateCanvasApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{templateId}/canvas")
    public ApiResponse<PortalTemplateCanvasView> current(
            @PathVariable String templateId,
            HttpServletRequest request
    ) {
        PortalTemplateCanvasView canvas = applicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template canvas not found"
        ));
        return ApiResponse.success(canvas, responseMetaFactory.create(request));
    }

    @PutMapping("/{templateId}/canvas")
    public ApiResponse<PortalTemplateCanvasView> save(
            @PathVariable String templateId,
            @Valid @RequestBody SavePortalTemplateCanvasRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.save(body.toCommand(templateId)),
                responseMetaFactory.create(request)
        );
    }
}
