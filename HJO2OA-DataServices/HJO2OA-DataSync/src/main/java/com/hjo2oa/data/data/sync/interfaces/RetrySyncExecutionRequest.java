package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.RetrySyncExecutionCommand;
import jakarta.validation.constraints.NotBlank;

public record RetrySyncExecutionRequest(
        String idempotencyKey,
        @NotBlank String operatorAccountId,
        String operatorPersonId,
        @NotBlank String reason
) {

    public RetrySyncExecutionCommand toCommand() {
        return new RetrySyncExecutionCommand(idempotencyKey, operatorAccountId, operatorPersonId, reason);
    }
}
