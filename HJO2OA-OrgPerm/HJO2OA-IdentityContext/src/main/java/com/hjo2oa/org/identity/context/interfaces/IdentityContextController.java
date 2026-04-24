package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.org.identity.context.application.IdentityContextQueryApplicationService;
import com.hjo2oa.org.identity.context.application.IdentityContextRefreshApplicationService;
import com.hjo2oa.org.identity.context.application.IdentityContextSwitchApplicationService;
import com.hjo2oa.org.identity.context.application.RefreshIdentityContextCommand;
import com.hjo2oa.org.identity.context.application.RefreshIdentityContextResult;
import com.hjo2oa.org.identity.context.application.ResetPrimaryIdentityContextCommand;
import com.hjo2oa.org.identity.context.application.SwitchIdentityContextCommand;
import com.hjo2oa.org.identity.context.domain.AvailableIdentityOption;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/org-perm/identity-context")
public class IdentityContextController {

    private final IdentityContextQueryApplicationService queryApplicationService;
    private final IdentityContextSwitchApplicationService switchApplicationService;
    private final IdentityContextRefreshApplicationService refreshApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public IdentityContextController(
            IdentityContextQueryApplicationService queryApplicationService,
            IdentityContextSwitchApplicationService switchApplicationService,
            IdentityContextRefreshApplicationService refreshApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.queryApplicationService = queryApplicationService;
        this.switchApplicationService = switchApplicationService;
        this.refreshApplicationService = refreshApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/current")
    public ApiResponse<IdentityContextView> current(HttpServletRequest request) {
        return ApiResponse.success(queryApplicationService.current(), responseMetaFactory.create(request));
    }

    @GetMapping("/available")
    public ApiResponse<List<AvailableIdentityOption>> available(
            @RequestParam(name = "includePrimary", defaultValue = "true") boolean includePrimary,
            HttpServletRequest request
    ) {
        return ApiResponse.success(queryApplicationService.available(includePrimary), responseMetaFactory.create(request));
    }

    @PostMapping("/switch")
    public ApiResponse<IdentityContextView> switchIdentity(
            @Valid @RequestBody SwitchIdentityContextRequest request,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                switchApplicationService.switchToSecondary(new SwitchIdentityContextCommand(
                        request.targetPositionId(),
                        request.reason()
                )),
                responseMetaFactory.create(servletRequest)
        );
    }

    @PostMapping("/reset-primary")
    public ApiResponse<IdentityContextView> resetPrimary(
            @RequestBody(required = false) ResetPrimaryIdentityContextRequest request,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.success(
                switchApplicationService.resetPrimary(new ResetPrimaryIdentityContextCommand(reason)),
                responseMetaFactory.create(servletRequest)
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshIdentityContextResult> refresh(
            @Valid @RequestBody RefreshIdentityContextRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                refreshApplicationService.refresh(new RefreshIdentityContextCommand(
                        request.tenantId(),
                        request.personId(),
                        request.accountId(),
                        request.invalidatedAssignmentId(),
                        request.fallbackAssignmentId(),
                        request.reasonCode(),
                        request.forceLogout(),
                        request.permissionSnapshotVersion(),
                        request.triggerEvent()
                )),
                responseMetaFactory.create(servletRequest)
        );
    }
}
