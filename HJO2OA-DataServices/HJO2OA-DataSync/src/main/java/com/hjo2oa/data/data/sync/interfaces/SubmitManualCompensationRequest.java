package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.SubmitManualCompensationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitManualCompensationRequest(
        String idempotencyKey,
        @NotBlank String operatorAccountId,
        String operatorPersonId,
        @NotBlank String reason,
        @NotEmpty List<@Valid CompensationDecisionRequest> decisions
) {

    public SubmitManualCompensationCommand toCommand() {
        return new SubmitManualCompensationCommand(
                idempotencyKey,
                operatorAccountId,
                operatorPersonId,
                reason,
                decisions.stream().map(CompensationDecisionRequest::toDecision).toList()
        );
    }
}
