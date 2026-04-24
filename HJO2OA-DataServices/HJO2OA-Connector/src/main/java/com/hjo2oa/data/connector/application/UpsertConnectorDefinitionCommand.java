package com.hjo2oa.data.connector.application;

import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;

public record UpsertConnectorDefinitionCommand(
        String connectorId,
        String code,
        String name,
        ConnectorType connectorType,
        String vendor,
        String protocol,
        ConnectorAuthMode authMode,
        TimeoutRetryConfig timeoutConfig
) {
}
