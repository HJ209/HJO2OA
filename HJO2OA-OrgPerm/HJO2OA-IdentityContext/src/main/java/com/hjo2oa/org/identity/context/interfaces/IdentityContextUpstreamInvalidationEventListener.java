package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.org.identity.context.application.IdentityContextUpstreamInvalidationApplicationService;
import com.hjo2oa.org.identity.context.domain.OrgAccountLockedEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentExpiredEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentRemovedEvent;
import com.hjo2oa.org.identity.context.domain.OrgPersonDisabledEvent;
import com.hjo2oa.org.identity.context.domain.OrgPositionDisabledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IdentityContextUpstreamInvalidationEventListener {

    private final IdentityContextUpstreamInvalidationApplicationService invalidationApplicationService;

    public IdentityContextUpstreamInvalidationEventListener(
            IdentityContextUpstreamInvalidationApplicationService invalidationApplicationService
    ) {
        this.invalidationApplicationService = invalidationApplicationService;
    }

    @EventListener
    public void onAssignmentRemoved(OrgAssignmentRemovedEvent event) {
        invalidationApplicationService.onAssignmentRemoved(event);
    }

    @EventListener
    public void onAssignmentExpired(OrgAssignmentExpiredEvent event) {
        invalidationApplicationService.onAssignmentExpired(event);
    }

    @EventListener
    public void onPositionDisabled(OrgPositionDisabledEvent event) {
        invalidationApplicationService.onPositionDisabled(event);
    }

    @EventListener
    public void onPersonDisabled(OrgPersonDisabledEvent event) {
        invalidationApplicationService.onPersonDisabled(event);
    }

    @EventListener
    public void onAccountLocked(OrgAccountLockedEvent event) {
        invalidationApplicationService.onAccountLocked(event);
    }
}
