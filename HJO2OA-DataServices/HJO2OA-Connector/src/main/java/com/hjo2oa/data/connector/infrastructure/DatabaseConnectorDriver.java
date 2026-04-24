package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplate;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplateCategory;
import com.hjo2oa.data.connector.domain.ConnectorTestResult;
import com.hjo2oa.data.connector.domain.ConnectorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectorDriver extends AbstractConnectorDriver {

    private final JdbcConnectivityClient jdbcConnectivityClient;

    public DatabaseConnectorDriver(
            ConnectorSecretValueResolver secretValueResolver,
            JdbcConnectivityClient jdbcConnectivityClient
    ) {
        super(secretValueResolver);
        this.jdbcConnectivityClient = jdbcConnectivityClient;
    }

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.DATABASE;
    }

    @Override
    public Set<ConnectorAuthMode> supportedAuthModes() {
        return Set.of(ConnectorAuthMode.BASIC, ConnectorAuthMode.SECRET_REF, ConnectorAuthMode.NONE);
    }

    @Override
    public List<ConnectorParameterTemplate> parameterTemplates(ConnectorDefinition connectorDefinition) {
        List<ConnectorParameterTemplate> templates = new ArrayList<>();
        templates.add(new ConnectorParameterTemplate(
                "jdbcUrl",
                "JDBC URL",
                ConnectorParameterTemplateCategory.ENDPOINT,
                true,
                false,
                "数据库连接地址"
        ));
        templates.add(new ConnectorParameterTemplate(
                "validationQuery",
                "Validation Query",
                ConnectorParameterTemplateCategory.HEALTH,
                false,
                false,
                "连通性测试默认使用 SELECT 1"
        ));
        switch (connectorDefinition.authMode()) {
            case BASIC -> {
                templates.add(new ConnectorParameterTemplate(
                        "username",
                        "Username",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        false,
                        "数据库用户名"
                ));
                templates.add(new ConnectorParameterTemplate(
                        "password",
                        "Password",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        true,
                        "数据库密码 keyRef"
                ));
            }
            case SECRET_REF -> {
                templates.add(new ConnectorParameterTemplate(
                        "username",
                        "Username",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        false,
                        "数据库用户名"
                ));
                templates.add(new ConnectorParameterTemplate(
                        "credentialRef",
                        "Credential Ref",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        true,
                        "数据库凭证 keyRef"
                ));
            }
            case NONE -> {
            }
            case TOKEN -> throw new IllegalArgumentException("TOKEN auth is not supported for DATABASE connectors");
        }
        return List.copyOf(templates);
    }

    @Override
    public ConnectorTestResult testConnection(ConnectorDefinition connectorDefinition) {
        return executeWithRetry(connectorDefinition, () -> {
            String jdbcUrl = requiredValue(connectorDefinition, "jdbcUrl");
            String validationQuery = optionalValue(connectorDefinition, "validationQuery", "SELECT 1");
            String username = "";
            String password = "";
            switch (connectorDefinition.authMode()) {
                case BASIC -> {
                    username = requiredValue(connectorDefinition, "username");
                    password = requiredValue(connectorDefinition, "password");
                }
                case SECRET_REF -> {
                    username = requiredValue(connectorDefinition, "username");
                    password = requiredValue(connectorDefinition, "credentialRef");
                }
                case NONE -> {
                }
                case TOKEN -> throw new IllegalArgumentException("TOKEN auth is not supported for DATABASE connectors");
            }
            jdbcConnectivityClient.validate(
                    jdbcUrl,
                    username,
                    password,
                    validationQuery,
                    connectorDefinition.timeoutConfig()
            );
        });
    }
}
