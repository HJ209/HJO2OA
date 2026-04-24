package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteView;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/designer/widget-palette")
public class PortalDesignerWidgetPaletteController {

    private final PortalDesignerWidgetPaletteApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalDesignerWidgetPaletteController(
            PortalDesignerWidgetPaletteApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<PortalDesignerWidgetPaletteView> currentPalette(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.currentPalette(),
                responseMetaFactory.create(request)
        );
    }
}
