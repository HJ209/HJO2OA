package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalActiveTemplateResolutionView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/model/resolutions")
public class PortalActiveTemplateResolutionController {

    private final PortalActiveTemplateResolutionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalActiveTemplateResolutionController(
            PortalActiveTemplateResolutionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/active")
    public ApiResponse<PortalActiveTemplateResolutionView> currentActive(
            @RequestParam PortalPublicationSceneType sceneType,
            @RequestParam PortalPublicationClientType clientType,
            @RequestParam(required = false) String assignmentId,
            @RequestParam(required = false) String positionId,
            @RequestParam(required = false) String personId,
            HttpServletRequest request
    ) {
        PortalActiveTemplateResolutionView resolution = applicationService.currentActive(
                        sceneType,
                        clientType,
                        new PortalPublicationIdentity(assignmentId, positionId, personId)
                )
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Active portal template resolution not found"
                ));
        return ApiResponse.success(resolution, responseMetaFactory.create(request));
    }
}
