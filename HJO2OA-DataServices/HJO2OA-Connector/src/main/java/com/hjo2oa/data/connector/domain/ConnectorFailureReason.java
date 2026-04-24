package com.hjo2oa.data.connector.domain;

public enum ConnectorFailureReason {
    TIMEOUT,
    AUTHENTICATION_FAILED,
    NETWORK_UNREACHABLE,
    CONFIGURATION_ERROR,
    DRIVER_ERROR,
    UNKNOWN
}
