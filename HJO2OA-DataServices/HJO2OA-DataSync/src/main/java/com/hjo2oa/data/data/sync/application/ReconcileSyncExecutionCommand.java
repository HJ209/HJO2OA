package com.hjo2oa.data.data.sync.application;

public record ReconcileSyncExecutionCommand(
        String idempotencyKey,
        String operatorAccountId,
        String operatorPersonId,
        String reason
) {
}
