package com.hjo2oa.data.connector.interfaces;

import com.hjo2oa.data.connector.application.ConnectorParameterValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConnectorParameterRequest(
        @NotBlank @Size(max = 64) String paramKey,
        @NotBlank @Size(max = 512) String paramValueRef,
        @NotNull Boolean sensitive
) {

    public ConnectorParameterValue toValue() {
        return new ConnectorParameterValue(paramKey, paramValueRef, Boolean.TRUE.equals(sensitive));
    }
}
