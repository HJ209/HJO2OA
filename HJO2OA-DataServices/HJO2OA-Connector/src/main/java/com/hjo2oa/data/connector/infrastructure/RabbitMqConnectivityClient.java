package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqConnectivityClient implements MessageQueueConnectivityClient {

    @Override
    public void validate(MessageQueueConnectionSpec connectionSpec, TimeoutRetryConfig timeoutRetryConfig) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(connectionSpec.host());
        connectionFactory.setPort(connectionSpec.port());
        connectionFactory.setVirtualHost(connectionSpec.virtualHost());
        if (connectionSpec.username() != null && !connectionSpec.username().isBlank()) {
            connectionFactory.setUsername(connectionSpec.username());
        }
        if (connectionSpec.password() != null && !connectionSpec.password().isBlank()) {
            connectionFactory.setPassword(connectionSpec.password());
        }
        connectionFactory.setConnectionTimeout(timeoutRetryConfig.connectTimeoutMs());
        connectionFactory.setHandshakeTimeout(timeoutRetryConfig.readTimeoutMs());

        try (Connection ignored = connectionFactory.newConnection("hjo2oa-connector-health-check")) {
            // Opening and closing the broker connection is enough for phase-1 connectivity verification.
        } catch (IOException ex) {
            throw mapIoException(ex);
        } catch (TimeoutException ex) {
            throw ConnectorConnectivityException.of(ConnectorFailureReason.TIMEOUT, "消息队列连接超时", ex);
        }
    }

    private RuntimeException mapIoException(IOException ex) {
        if (ex.getCause() instanceof SocketTimeoutException) {
            return ConnectorConnectivityException.of(ConnectorFailureReason.TIMEOUT, "消息队列连接超时", ex);
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("access refused") || message.contains("auth")) {
            return ConnectorConnectivityException.of(ConnectorFailureReason.AUTHENTICATION_FAILED, "消息队列认证失败", ex);
        }
        return ConnectorConnectivityException.of(ConnectorFailureReason.NETWORK_UNREACHABLE, "消息队列连接失败", ex);
    }
}
