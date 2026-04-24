package com.hjo2oa.data.data.sync.application;

public record ResetSyncCheckpointCommand(
        String checkpointValue,
        String operatorAccountId,
        String reason
) {
}
