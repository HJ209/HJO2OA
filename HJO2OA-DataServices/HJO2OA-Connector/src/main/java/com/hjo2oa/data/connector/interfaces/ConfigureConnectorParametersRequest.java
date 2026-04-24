package com.hjo2oa.data.connector.interfaces;

import com.hjo2oa.data.connector.application.ConfigureConnectorParametersCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConfigureConnectorParametersRequest(
        @NotNull List<@Valid ConnectorParameterRequest> parameters
) {

    public ConfigureConnectorParametersRequest {
        parameters = List.copyOf(parameters);
    }

    public ConfigureConnectorParametersCommand toCommand(String connectorId) {
        return new ConfigureConnectorParametersCommand(
                connectorId,
                parameters.stream().map(ConnectorParameterRequest::toValue).toList()
        );
    }
}
