package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorFailureReason;

final class ConnectorConnectivityException extends RuntimeException {

    private final ConnectorFailureReason failureReason;

    private ConnectorConnectivityException(ConnectorFailureReason failureReason, String message, Throwable cause) {
        super(message, cause);
        this.failureReason = failureReason;
    }

    static ConnectorConnectivityException of(ConnectorFailureReason failureReason, String message) {
        return new ConnectorConnectivityException(failureReason, message, null);
    }

    static ConnectorConnectivityException of(ConnectorFailureReason failureReason, String message, Throwable cause) {
        return new ConnectorConnectivityException(failureReason, message, cause);
    }

    ConnectorFailureReason failureReason() {
        return failureReason;
    }
}
