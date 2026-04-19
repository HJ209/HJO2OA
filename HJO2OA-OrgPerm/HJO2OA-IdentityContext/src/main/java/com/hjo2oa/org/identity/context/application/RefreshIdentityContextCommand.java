package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;

public record RefreshIdentityContextCommand(
        String tenantId,
        String personId,
        String accountId,
        String invalidatedAssignmentId,
        String fallbackAssignmentId,
        IdentityContextInvalidationReason reasonCode,
        boolean forceLogout,
        long permissionSnapshotVersion,
        String triggerEvent
) {
}
