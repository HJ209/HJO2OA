package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.stereotype.Component;

@Component
public class DriverManagerJdbcConnectivityClient implements JdbcConnectivityClient {

    @Override
    public void validate(
            String jdbcUrl,
            String username,
            String password,
            String validationQuery,
            TimeoutRetryConfig timeoutRetryConfig
    ) {
        try {
            DriverManager.setLoginTimeout(Math.max(1, timeoutRetryConfig.connectTimeoutMs() / 1000));
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(Math.max(1, timeoutRetryConfig.readTimeoutMs() / 1000));
                statement.execute(validationQuery);
            }
        } catch (SQLException ex) {
            throw mapSqlException(ex);
        }
    }

    private RuntimeException mapSqlException(SQLException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("login failed") || message.contains("authentication")) {
            return ConnectorConnectivityException.of(ConnectorFailureReason.AUTHENTICATION_FAILED, "数据库认证失败", ex);
        }
        if (message.contains("timeout")) {
            return ConnectorConnectivityException.of(ConnectorFailureReason.TIMEOUT, "数据库连接超时", ex);
        }
        if (message.contains("no suitable driver")) {
            return ConnectorConnectivityException.of(ConnectorFailureReason.DRIVER_ERROR, "数据库驱动不可用", ex);
        }
        return ConnectorConnectivityException.of(ConnectorFailureReason.NETWORK_UNREACHABLE, "数据库连接失败", ex);
    }
}
