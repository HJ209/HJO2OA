package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.ReconcileSyncExecutionCommand;
import jakarta.validation.constraints.NotBlank;

public record ReconcileSyncExecutionRequest(
        String idempotencyKey,
        @NotBlank String operatorAccountId,
        String operatorPersonId,
        @NotBlank String reason
) {

    public ReconcileSyncExecutionCommand toCommand() {
        return new ReconcileSyncExecutionCommand(idempotencyKey, operatorAccountId, operatorPersonId, reason);
    }
}
