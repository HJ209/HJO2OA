package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.DeprecatePortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/model/templates")
public class PortalTemplateController {

    private final PortalTemplateApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalTemplateController(
            PortalTemplateApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<List<PortalTemplateView>> list(
            @RequestParam(required = false) com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType sceneType,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.list(sceneType),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{templateId}")
    public ApiResponse<PortalTemplateView> current(
            @PathVariable String templateId,
            HttpServletRequest request
    ) {
        PortalTemplateView template = applicationService.current(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        return ApiResponse.success(template, responseMetaFactory.create(request));
    }

    @PostMapping
    public ApiResponse<PortalTemplateView> create(
            @Valid @RequestBody CreatePortalTemplateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.create(body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{templateId}/publish")
    public ApiResponse<PortalTemplateView> publish(
            @PathVariable String templateId,
            @Valid @RequestBody PublishPortalTemplateVersionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.publish(body.toCommand(templateId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{templateId}/versions/{versionNo}/deprecate")
    public ApiResponse<PortalTemplateView> deprecate(
            @PathVariable String templateId,
            @PathVariable int versionNo,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.deprecate(new DeprecatePortalTemplateVersionCommand(templateId, versionNo)),
                responseMetaFactory.create(request)
        );
    }
}
