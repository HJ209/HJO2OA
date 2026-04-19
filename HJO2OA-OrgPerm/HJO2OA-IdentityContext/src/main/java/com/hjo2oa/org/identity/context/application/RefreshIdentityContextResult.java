package com.hjo2oa.org.identity.context.application;

import java.time.Instant;
import java.util.UUID;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;

public record RefreshIdentityContextResult(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String invalidatedAssignmentId,
        String fallbackAssignmentId,
        boolean forceLogout,
        long permissionSnapshotVersion,
        IdentityContextView currentContext
) {
}
