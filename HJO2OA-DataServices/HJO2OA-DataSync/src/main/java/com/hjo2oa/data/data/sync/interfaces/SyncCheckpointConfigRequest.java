package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;

public record SyncCheckpointConfigRequest(
        String checkpointField,
        String idempotencyField,
        boolean allowManualReset,
        String initialValue
) {

    public SyncCheckpointConfig toConfig() {
        return new SyncCheckpointConfig(
                checkpointField,
                idempotencyField,
                allowManualReset,
                initialValue,
                null,
                null,
                null,
                null
        );
    }
}
