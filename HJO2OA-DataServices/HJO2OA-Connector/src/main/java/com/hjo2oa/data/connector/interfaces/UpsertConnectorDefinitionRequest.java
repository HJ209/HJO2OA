package com.hjo2oa.data.connector.interfaces;

import com.hjo2oa.data.connector.application.UpsertConnectorDefinitionCommand;
import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertConnectorDefinitionRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull ConnectorType connectorType,
        @Size(max = 64) String vendor,
        @Size(max = 32) String protocol,
        @NotNull ConnectorAuthMode authMode,
        @Valid TimeoutRetryConfigRequest timeoutConfig
) {

    public UpsertConnectorDefinitionCommand toCommand(String connectorId) {
        return new UpsertConnectorDefinitionCommand(
                connectorId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig == null ? null : timeoutConfig.toValue()
        );
    }
}
