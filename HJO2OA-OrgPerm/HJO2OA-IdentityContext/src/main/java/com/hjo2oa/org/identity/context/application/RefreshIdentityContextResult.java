package com.hjo2oa.org.identity.context.application;

import java.time.Instant;
import java.util.Objects;
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
        RefreshIdentityContextOutcome outcome,
        IdentityContextView currentContext
) {

    public RefreshIdentityContextResult {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (permissionSnapshotVersion < 0) {
            throw new IllegalArgumentException("permissionSnapshotVersion must not be negative");
        }
        if (outcome == RefreshIdentityContextOutcome.RELOGIN_REQUIRED && currentContext != null) {
            throw new IllegalArgumentException("currentContext must be null when outcome is RELOGIN_REQUIRED");
        }
        if (outcome != RefreshIdentityContextOutcome.RELOGIN_REQUIRED && currentContext == null) {
            throw new IllegalArgumentException("currentContext must not be null when outcome keeps the session active");
        }
    }
}
