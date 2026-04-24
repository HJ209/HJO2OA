package com.hjo2oa.data.connector.application;

import java.util.List;

public record ConfigureConnectorParametersCommand(
        String connectorId,
        List<ConnectorParameterValue> parameters
) {

    public ConfigureConnectorParametersCommand {
        parameters = List.copyOf(parameters);
    }
}
