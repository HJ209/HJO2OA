package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorDriver;
import com.hjo2oa.data.connector.domain.ConnectorFailureReason;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplate;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplateCategory;
import com.hjo2oa.data.connector.domain.ConnectorTestResult;
import com.hjo2oa.data.connector.domain.ConnectorType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class HttpConnectorDriver extends AbstractConnectorDriver {

    private final HttpConnectivityClient httpConnectivityClient;

    public HttpConnectorDriver(
            ConnectorSecretValueResolver secretValueResolver,
            HttpConnectivityClient httpConnectivityClient
    ) {
        super(secretValueResolver);
        this.httpConnectivityClient = httpConnectivityClient;
    }

    @Override
    public ConnectorType connectorType() {
        return ConnectorType.HTTP;
    }

    @Override
    public Set<ConnectorAuthMode> supportedAuthModes() {
        return Set.of(
                ConnectorAuthMode.BASIC,
                ConnectorAuthMode.TOKEN,
                ConnectorAuthMode.SECRET_REF,
                ConnectorAuthMode.NONE
        );
    }

    @Override
    public List<ConnectorParameterTemplate> parameterTemplates(ConnectorDefinition connectorDefinition) {
        List<ConnectorParameterTemplate> templates = new ArrayList<>();
        templates.add(new ConnectorParameterTemplate(
                "baseUrl",
                "Base URL",
                ConnectorParameterTemplateCategory.ENDPOINT,
                true,
                false,
                "HTTP 连接器基础地址"
        ));
        templates.add(new ConnectorParameterTemplate(
                "healthPath",
                "Health Path",
                ConnectorParameterTemplateCategory.HEALTH,
                false,
                false,
                "用于连通性测试的相对路径，默认 /"
        ));
        templates.add(new ConnectorParameterTemplate(
                "method",
                "Method",
                ConnectorParameterTemplateCategory.HEALTH,
                false,
                false,
                "连通性测试默认使用 GET"
        ));
        appendAuthTemplates(connectorDefinition.authMode(), templates);
        return List.copyOf(templates);
    }

    @Override
    public ConnectorTestResult testConnection(ConnectorDefinition connectorDefinition) {
        return executeWithRetry(connectorDefinition, () -> {
            String baseUrl = requiredValue(connectorDefinition, "baseUrl");
            String healthPath = optionalValue(connectorDefinition, "healthPath", "/");
            String method = optionalValue(connectorDefinition, "method", "GET").toUpperCase();
            Map<String, String> headers = authHeaders(connectorDefinition);
            int statusCode = httpConnectivityClient.execute(new HttpRequestSpec(
                    combineUrl(baseUrl, healthPath),
                    method,
                    headers,
                    connectorDefinition.timeoutConfig()
            ));
            if (statusCode == 401 || statusCode == 403) {
                throw ConnectorConnectivityException.of(ConnectorFailureReason.AUTHENTICATION_FAILED, "HTTP 认证失败");
            }
            if (statusCode >= 400 && statusCode < 500) {
                throw ConnectorConnectivityException.of(ConnectorFailureReason.CONFIGURATION_ERROR, "HTTP 端点配置不可用");
            }
            if (statusCode >= 500) {
                throw ConnectorConnectivityException.of(ConnectorFailureReason.UNKNOWN, "HTTP 服务端返回异常状态码: " + statusCode);
            }
        });
    }

    private void appendAuthTemplates(ConnectorAuthMode authMode, List<ConnectorParameterTemplate> templates) {
        switch (authMode) {
            case BASIC -> {
                templates.add(new ConnectorParameterTemplate(
                        "username",
                        "Username",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        false,
                        "HTTP Basic 用户名"
                ));
                templates.add(new ConnectorParameterTemplate(
                        "password",
                        "Password",
                        ConnectorParameterTemplateCategory.AUTH,
                        true,
                        true,
                        "HTTP Basic 密码 keyRef"
                ));
            }
            case TOKEN -> templates.add(new ConnectorParameterTemplate(
                    "token",
                    "Token",
                    ConnectorParameterTemplateCategory.AUTH,
                    true,
                    true,
                    "Bearer Token keyRef"
            ));
            case SECRET_REF -> templates.add(new ConnectorParameterTemplate(
                    "credentialRef",
                    "Credential Ref",
                    ConnectorParameterTemplateCategory.AUTH,
                    true,
                    true,
                    "统一密钥引用，用于令牌型认证"
            ));
            case NONE -> {
            }
        }
    }

    private Map<String, String> authHeaders(ConnectorDefinition connectorDefinition) {
        Map<String, String> headers = new LinkedHashMap<>();
        switch (connectorDefinition.authMode()) {
            case BASIC -> {
                String username = requiredValue(connectorDefinition, "username");
                String password = requiredValue(connectorDefinition, "password");
                String raw = username + ":" + password;
                headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            }
            case TOKEN -> headers.put("Authorization", "Bearer " + requiredValue(connectorDefinition, "token"));
            case SECRET_REF -> headers.put("Authorization", "Bearer " + requiredValue(connectorDefinition, "credentialRef"));
            case NONE -> {
            }
        }
        return headers;
    }

    private String combineUrl(String baseUrl, String healthPath) {
        if (baseUrl.endsWith("/") && healthPath.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + healthPath;
        }
        if (!baseUrl.endsWith("/") && !healthPath.startsWith("/")) {
            return baseUrl + "/" + healthPath;
        }
        return baseUrl + healthPath;
    }
}
