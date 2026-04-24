package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.CompensationAction;
import com.hjo2oa.data.data.sync.domain.SyncCompensationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompensationDecisionRequest(
        @NotBlank String differenceCode,
        @NotNull CompensationAction action,
        @NotBlank String reason
) {

    public SyncCompensationDecision toDecision() {
        return new SyncCompensationDecision(differenceCode, action, reason);
    }
}
