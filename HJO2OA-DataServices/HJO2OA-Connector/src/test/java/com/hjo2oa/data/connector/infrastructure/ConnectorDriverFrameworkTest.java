package com.hjo2oa.data.connector.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import com.hjo2oa.data.connector.domain.ConnectorTestResult;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConnectorDriverFrameworkTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T04:00:00Z");

    @Test
    void shouldRetryHttpConnectivityWithinConfiguredRetryPolicy() {
        AtomicInteger attempts = new AtomicInteger();
        HttpConnectorDriver driver = new HttpConnectorDriver(
                testSecretResolver(),
                requestSpec -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw ConnectorConnectivityException.of(ConnectorFailureReason.NETWORK_UNREACHABLE, "first failure");
                    }
                    return 200;
                }
        );

        ConnectorTestResult result = driver.testConnection(httpConnector(
                new TimeoutRetryConfig(1_000, 1_000, 1, 0),
                List.of(
                        new com.hjo2oa.data.connector.domain.ConnectorParameter(
                                "p1", "c-http", "baseUrl", "https://example.test", false
                        ),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter(
                                "p2", "c-http", "token", "keyRef:http.demo.token", true
                        )
                ),
                ConnectorAuthMode.TOKEN
        ));

        assertThat(result.healthy()).isTrue();
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shouldClassifyDatabaseAuthenticationFailure() {
        DatabaseConnectorDriver driver = new DatabaseConnectorDriver(
                testSecretResolver(),
                (jdbcUrl, username, password, validationQuery, timeoutRetryConfig) -> {
                    throw ConnectorConnectivityException.of(ConnectorFailureReason.AUTHENTICATION_FAILED, "bad credentials");
                }
        );

        ConnectorTestResult result = driver.testConnection(databaseConnector());

        assertThat(result.healthy()).isFalse();
        assertThat(result.errorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void shouldValidateMessageQueueConnectivity() {
        AtomicInteger attempts = new AtomicInteger();
        MessageQueueConnectorDriver driver = new MessageQueueConnectorDriver(
                testSecretResolver(),
                (connectionSpec, timeoutRetryConfig) -> attempts.incrementAndGet()
        );

        ConnectorTestResult result = driver.testConnection(messageQueueConnector());

        assertThat(result.healthy()).isTrue();
        assertThat(attempts.get()).isEqualTo(1);
    }

    private ConnectorDefinition httpConnector(
            TimeoutRetryConfig timeoutRetryConfig,
            List<com.hjo2oa.data.connector.domain.ConnectorParameter> parameters,
            ConnectorAuthMode authMode
    ) {
        return new ConnectorDefinition(
                "c-http",
                "tenant-1",
                "http-test",
                "HTTP Test",
                ConnectorType.HTTP,
                "demo",
                "https",
                authMode,
                timeoutRetryConfig,
                com.hjo2oa.data.connector.domain.ConnectorStatus.DRAFT,
                0L,
                parameters,
                FIXED_TIME,
                FIXED_TIME
        );
    }

    private ConnectorDefinition databaseConnector() {
        return new ConnectorDefinition(
                "c-db",
                "tenant-1",
                "db-test",
                "DB Test",
                ConnectorType.DATABASE,
                "mssql",
                "jdbc",
                ConnectorAuthMode.BASIC,
                TimeoutRetryConfig.defaultConfig(),
                com.hjo2oa.data.connector.domain.ConnectorStatus.DRAFT,
                0L,
                List.of(
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p1", "c-db", "jdbcUrl", "jdbc:sqlserver://localhost", false),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p2", "c-db", "username", "sa", false),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p3", "c-db", "password", "keyRef:db.password", true)
                ),
                FIXED_TIME,
                FIXED_TIME
        );
    }

    private ConnectorDefinition messageQueueConnector() {
        return new ConnectorDefinition(
                "c-mq",
                "tenant-1",
                "mq-test",
                "MQ Test",
                ConnectorType.MQ,
                "rabbitmq",
                "amqp",
                ConnectorAuthMode.BASIC,
                TimeoutRetryConfig.defaultConfig(),
                com.hjo2oa.data.connector.domain.ConnectorStatus.DRAFT,
                0L,
                List.of(
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p1", "c-mq", "host", "localhost", false),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p2", "c-mq", "port", "5672", false),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p3", "c-mq", "username", "guest", false),
                        new com.hjo2oa.data.connector.domain.ConnectorParameter("p4", "c-mq", "password", "keyRef:mq.password", true)
                ),
                FIXED_TIME,
                FIXED_TIME
        );
    }

    private ConnectorSecretValueResolver testSecretResolver() {
        return (paramValueRef, sensitive) -> {
            if (!sensitive) {
                return paramValueRef;
            }
            return switch (paramValueRef) {
                case "keyRef:http.demo.token" -> "token-1";
                case "keyRef:db.password" -> "db-password";
                case "keyRef:mq.password" -> "mq-password";
                default -> throw new IllegalArgumentException("Unexpected keyRef: " + paramValueRef);
            };
        };
    }
}
