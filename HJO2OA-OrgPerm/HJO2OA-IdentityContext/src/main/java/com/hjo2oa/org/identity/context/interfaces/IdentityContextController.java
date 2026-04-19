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
@RequestMapping("/api/org-perm/identity-context")
public class IdentityContextController {

    private final IdentityContextQueryApplicationService queryApplicationService;
    private final IdentityContextSwitchApplicationService switchApplicationService;
    private final IdentityContextRefreshApplicationService refreshApplicationService;

    public IdentityContextController(
            IdentityContextQueryApplicationService queryApplicationService,
            IdentityContextSwitchApplicationService switchApplicationService,
            IdentityContextRefreshApplicationService refreshApplicationService
    ) {
        this.queryApplicationService = queryApplicationService;
        this.switchApplicationService = switchApplicationService;
        this.refreshApplicationService = refreshApplicationService;
    }

    @GetMapping("/current")
    public IdentityContextView current() {
        return queryApplicationService.current();
    }

    @GetMapping("/available")
    public List<AvailableIdentityOption> available(
            @RequestParam(name = "includePrimary", defaultValue = "true") boolean includePrimary
    ) {
        return queryApplicationService.available(includePrimary);
    }

    @PostMapping("/switch")
    public IdentityContextView switchIdentity(
            @Valid @RequestBody SwitchIdentityContextRequest request,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        return switchApplicationService.switchToSecondary(new SwitchIdentityContextCommand(
                request.targetPositionId(),
                request.reason()
        ));
    }

    @PostMapping("/reset-primary")
    public IdentityContextView resetPrimary(
            @RequestBody(required = false) ResetPrimaryIdentityContextRequest request,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String reason = request == null ? null : request.reason();
        return switchApplicationService.resetPrimary(new ResetPrimaryIdentityContextCommand(reason));
    }

    @PostMapping("/refresh")
    public RefreshIdentityContextResult refresh(@Valid @RequestBody RefreshIdentityContextRequest request) {
        return refreshApplicationService.refresh(new RefreshIdentityContextCommand(
                request.tenantId(),
                request.personId(),
                request.accountId(),
                request.invalidatedAssignmentId(),
                request.fallbackAssignmentId(),
                request.reasonCode(),
                request.forceLogout(),
                request.permissionSnapshotVersion(),
                request.triggerEvent()
        ));
    }
}
