package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
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
@RequestMapping("/api/v1/portal/model/publications")
public class PortalModelController {

    private final PortalPublicationApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalModelController(
            PortalPublicationApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<List<PortalPublicationView>> list(
            @RequestParam(required = false) PortalPublicationSceneType sceneType,
            @RequestParam(required = false) PortalPublicationClientType clientType,
            @RequestParam(required = false) PortalPublicationStatus status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.list(sceneType, clientType, status),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{publicationId}")
    public ApiResponse<PortalPublicationView> current(
            @PathVariable String publicationId,
            HttpServletRequest request
    ) {
        PortalPublicationView publication = applicationService.current(publicationId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal publication not found"
                ));
        return ApiResponse.success(publication, responseMetaFactory.create(request));
    }

    @GetMapping("/active")
    public ApiResponse<PortalPublicationView> currentActive(
            @RequestParam PortalPublicationSceneType sceneType,
            @RequestParam PortalPublicationClientType clientType,
            @RequestParam(required = false) String assignmentId,
            @RequestParam(required = false) String positionId,
            @RequestParam(required = false) String personId,
            HttpServletRequest request
    ) {
        PortalPublicationView publication = applicationService.currentActive(
                        sceneType,
                        clientType,
                        new PortalPublicationIdentity(assignmentId, positionId, personId)
                )
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Active portal publication not found"
                ));
        return ApiResponse.success(publication, responseMetaFactory.create(request));
    }

    @PutMapping("/{publicationId}/activate")
    public ApiResponse<PortalPublicationView> activate(
            @PathVariable String publicationId,
            @Valid @RequestBody ActivatePortalPublicationRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.activate(body.toCommand(publicationId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{publicationId}/offline")
    public ApiResponse<PortalPublicationView> offline(
            @PathVariable String publicationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.offline(new OfflinePortalPublicationCommand(publicationId)),
                responseMetaFactory.create(request)
        );
    }
}
