package com.hjo2oa.data.data.sync.domain;

import java.util.Objects;

public record SyncCompensationDecision(
        String differenceCode,
        CompensationAction action,
        String reason
) {

    public SyncCompensationDecision {
        differenceCode = SyncDomainSupport.requireText(differenceCode, "differenceCode");
        Objects.requireNonNull(action, "action must not be null");
        reason = SyncDomainSupport.requireText(reason, "reason");
    }
}
