package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorDriver;
import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import com.hjo2oa.data.connector.domain.ConnectorParameter;
import com.hjo2oa.data.connector.domain.ConnectorTestResult;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

abstract class AbstractConnectorDriver implements ConnectorDriver {

    private final ConnectorSecretValueResolver secretValueResolver;

    protected AbstractConnectorDriver(ConnectorSecretValueResolver secretValueResolver) {
        this.secretValueResolver = Objects.requireNonNull(secretValueResolver, "secretValueResolver must not be null");
    }

    protected ConnectorTestResult executeWithRetry(
            ConnectorDefinition connectorDefinition,
            ThrowingRunnable connectivityAction
    ) {
        TimeoutRetryConfig timeoutConfig = connectorDefinition.timeoutConfig();
        long startedAt = System.nanoTime();
        int totalAttempts = timeoutConfig.retryCount() + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                connectivityAction.run();
                return ConnectorTestResult.healthy(elapsedMillis(startedAt));
            } catch (Exception ex) {
                if (attempt >= totalAttempts) {
                    return ConnectorTestResult.unhealthy(
                            elapsedMillis(startedAt),
                            classifyFailure(ex),
                            safeMessage(ex)
                    );
                }
                sleepBeforeRetry(timeoutConfig.retryIntervalMs());
            }
        }
        return ConnectorTestResult.unhealthy(
                elapsedMillis(startedAt),
                ConnectorFailureReason.UNKNOWN,
                "连接测试执行结束但未得到结果"
        );
    }

    protected Map<String, ConnectorParameter> parameterMap(ConnectorDefinition connectorDefinition) {
        return connectorDefinition.parameters().stream()
                .collect(Collectors.toMap(ConnectorParameter::paramKey, Function.identity()));
    }

    protected String requiredValue(ConnectorDefinition connectorDefinition, String paramKey) {
        ConnectorParameter parameter = parameterMap(connectorDefinition).get(paramKey);
        if (parameter == null) {
            throw new IllegalArgumentException("Missing connector parameter: " + paramKey);
        }
        return secretValueResolver.resolve(parameter.paramValueRef(), parameter.sensitive());
    }

    protected String optionalValue(ConnectorDefinition connectorDefinition, String paramKey, String defaultValue) {
        ConnectorParameter parameter = parameterMap(connectorDefinition).get(paramKey);
        if (parameter == null) {
            return defaultValue;
        }
        return secretValueResolver.resolve(parameter.paramValueRef(), parameter.sensitive());
    }

    protected int optionalIntegerValue(ConnectorDefinition connectorDefinition, String paramKey, int defaultValue) {
        String value = optionalValue(connectorDefinition, paramKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw ConnectorConnectivityException.of(
                    ConnectorFailureReason.CONFIGURATION_ERROR,
                    "Invalid integer connector parameter: " + paramKey,
                    ex
            );
        }
    }

    protected ConnectorFailureReason classifyFailure(Exception ex) {
        if (ex instanceof ConnectorConnectivityException connectivityException) {
            return connectivityException.failureReason();
        }
        if (ex instanceof IllegalArgumentException) {
            return ConnectorFailureReason.CONFIGURATION_ERROR;
        }
        if (ex instanceof SocketTimeoutException
                || ex instanceof HttpTimeoutException
                || ex instanceof SQLTimeoutException) {
            return ConnectorFailureReason.TIMEOUT;
        }
        if (ex instanceof UnknownHostException || ex instanceof ConnectException) {
            return ConnectorFailureReason.NETWORK_UNREACHABLE;
        }
        if (ex instanceof SQLInvalidAuthorizationSpecException) {
            return ConnectorFailureReason.AUTHENTICATION_FAILED;
        }
        return ConnectorFailureReason.UNKNOWN;
    }

    protected String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private void sleepBeforeRetry(long retryIntervalMs) {
        if (retryIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryIntervalMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ConnectorConnectivityException.of(
                    ConnectorFailureReason.UNKNOWN,
                    "连接测试重试被中断",
                    ex
            );
        }
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }
}
