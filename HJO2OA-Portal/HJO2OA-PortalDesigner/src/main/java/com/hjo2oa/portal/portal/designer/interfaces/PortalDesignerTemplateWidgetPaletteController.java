package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteView;
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
public class PortalDesignerTemplateWidgetPaletteController {

    private final PortalDesignerTemplateWidgetPaletteApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerTemplateWidgetPaletteController(
            PortalDesignerTemplateWidgetPaletteApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{templateId}/widget-palette")
    public ApiResponse<PortalDesignerTemplateWidgetPaletteView> current(
            @PathVariable String templateId,
            HttpServletRequest request
    ) {
        PortalDesignerTemplateWidgetPaletteView palette = applicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal designer template widget palette not found"
                ));
        return ApiResponse.success(palette, responseMetaFactory.create(request));
    }
}
