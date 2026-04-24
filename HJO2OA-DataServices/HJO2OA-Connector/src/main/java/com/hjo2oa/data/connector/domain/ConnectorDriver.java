package com.hjo2oa.data.connector.domain;

import java.util.List;
import java.util.Set;

public interface ConnectorDriver {

    ConnectorType connectorType();

    Set<ConnectorAuthMode> supportedAuthModes();

    List<ConnectorParameterTemplate> parameterTemplates(ConnectorDefinition connectorDefinition);

    ConnectorTestResult testConnection(ConnectorDefinition connectorDefinition);
}
