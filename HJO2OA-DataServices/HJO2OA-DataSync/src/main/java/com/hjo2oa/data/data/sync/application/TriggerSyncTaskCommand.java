package com.hjo2oa.data.data.sync.application;

import java.util.Map;

public record TriggerSyncTaskCommand(
        String idempotencyKey,
        String operatorAccountId,
        String operatorPersonId,
        Map<String, Object> triggerContext
) {
}
