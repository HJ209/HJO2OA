package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.ResetSyncCheckpointCommand;
import jakarta.validation.constraints.NotBlank;

public record ResetSyncCheckpointRequest(
        @NotBlank String checkpointValue,
        @NotBlank String operatorAccountId,
        @NotBlank String reason
) {

    public ResetSyncCheckpointCommand toCommand() {
        return new ResetSyncCheckpointCommand(checkpointValue, operatorAccountId, reason);
    }
}
