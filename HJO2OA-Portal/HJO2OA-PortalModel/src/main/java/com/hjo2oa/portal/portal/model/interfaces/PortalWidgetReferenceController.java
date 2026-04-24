package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatus;
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
@RequestMapping("/api/v1/portal/model/widget-references")
public class PortalWidgetReferenceController {

    private final PortalWidgetReferenceStatusApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalWidgetReferenceController(
            PortalWidgetReferenceStatusApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{widgetId}")
    public ApiResponse<PortalWidgetReferenceStatus> current(
            @PathVariable String widgetId,
            HttpServletRequest request
    ) {
        PortalWidgetReferenceStatus status = applicationService.current(widgetId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal widget reference status not found"
                ));
        return ApiResponse.success(status, responseMetaFactory.create(request));
    }
}
