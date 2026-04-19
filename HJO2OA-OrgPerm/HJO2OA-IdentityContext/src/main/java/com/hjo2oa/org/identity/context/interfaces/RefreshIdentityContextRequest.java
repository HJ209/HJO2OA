package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RefreshIdentityContextRequest(
        @NotBlank String tenantId,
        @NotBlank String personId,
        @NotBlank String accountId,
        @NotBlank String invalidatedAssignmentId,
        String fallbackAssignmentId,
        @NotNull IdentityContextInvalidationReason reasonCode,
        boolean forceLogout,
        @PositiveOrZero long permissionSnapshotVersion,
        @NotBlank String triggerEvent
) {
}
