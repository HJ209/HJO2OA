package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.SyncCompensationDecision;
import java.util.List;

public record SubmitManualCompensationCommand(
        String idempotencyKey,
        String operatorAccountId,
        String operatorPersonId,
        String reason,
        List<SyncCompensationDecision> decisions
) {
}
