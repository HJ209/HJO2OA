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
public class MessageQueueConnectorDriver extends AbstractConnectorDriver {

    private final MessageQueueConnectivityClient messageQueueConnectivityClient;

    public MessageQueueConnectorDriver(
            ConnectorSecretValueResolver secretValueResolver,
            MessageQueueConnectivityClient messageQueueConnectivityClient
    ) {
        super(secretValueResolver);
        this.messageQueueConnectivityClient = messageQueueConnectivityClient;
    }

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.MQ;
    }

    @Override
    public Set<ConnectorAuthMode> supportedAuthModes() {
        return Set.of(ConnectorAuthMode.BASIC, ConnectorAuthMode.SECRET_REF, ConnectorAuthMode.NONE);
    }

    @Override
    public List<ConnectorParameterTemplate> parameterTemplates(ConnectorDefinition connectorDefinition) {
        List<ConnectorParameterTemplate> templates = new ArrayList<>();
        templates.add(new ConnectorParameterTemplate(
                "host",
                "Host",
                ConnectorParameterTemplateCategory.ENDPOINT,
                true,
                false,
                "消息队列主机地址"
        ));
        templates.add(new ConnectorParameterTemplate(
                "port",
                "Port",
                ConnectorParameterTemplateCategory.ENDPOINT,
                false,
                false,
                "端口，默认 5672"
        ));
        templates.add(new ConnectorParameterTemplate(
                "virtualHost",
                "Virtual Host",
                ConnectorParameterTemplateCategory.ENDPOINT,
                false,
                false,
                "RabbitMQ 虚拟主机，默认 /"
        ));
        switch (connectorDefinition.authMode()) {
            case BASIC -> {
                templates.add(new ConnectorParameterTemplate(
                        "username",
                        "Username",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        false,
                        "消息队列用户名"
                ));
                templates.add(new ConnectorParameterTemplate(
                        "password",
                        "Password",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        true,
                        "消息队列密码 keyRef"
                ));
            }
            case SECRET_REF -> {
                templates.add(new ConnectorParameterTemplate(
                        "username",
                        "Username",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        false,
                        "消息队列用户名"
                ));
                templates.add(new ConnectorParameterTemplate(
                        "credentialRef",
                        "Credential Ref",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        true,
                        "消息队列统一密钥引用"
                ));
            }
            case NONE -> {
            }
            case TOKEN -> throw new IllegalArgumentException("TOKEN auth is not supported for MQ connectors");
        }
        return List.copyOf(templates);
    }

    @Override
    public ConnectorTestResult testConnection(ConnectorDefinition connectorDefinition) {
        return executeWithRetry(connectorDefinition, () -> {
            String host = requiredValue(connectorDefinition, "host");
            int port = optionalIntegerValue(connectorDefinition, "port", 5672);
            String virtualHost = optionalValue(connectorDefinition, "virtualHost", "/");
            String username = null;
            String password = null;
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
                case TOKEN -> throw new IllegalArgumentException("TOKEN auth is not supported for MQ connectors");
            }
            messageQueueConnectivityClient.validate(
                    new MessageQueueConnectionSpec(host, port, virtualHost, username, password),
                    connectorDefinition.timeoutConfig()
            );
        });
    }
}
