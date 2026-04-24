package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.TriggerSyncTaskCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TriggerSyncTaskRequest(
        String idempotencyKey,
        @NotBlank String operatorAccountId,
        String operatorPersonId,
        Map<String, Object> triggerContext
) {

    public TriggerSyncTaskCommand toCommand() {
        return new TriggerSyncTaskCommand(idempotencyKey, operatorAccountId, operatorPersonId, triggerContext);
    }
}
