package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorParameter;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentConnectorSecretValueResolver implements ConnectorSecretValueResolver {

    private static final String SECRET_PROPERTY_PREFIX = "hjo2oa.connector.secret-values.";

    private final Environment environment;

    public EnvironmentConnectorSecretValueResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String paramValueRef, boolean sensitive) {
        if (!sensitive) {
            return paramValueRef;
        }
        if (!ConnectorParameter.isKeyReference(paramValueRef)) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "敏感连接参数必须使用 keyRef 引用"
            );
        }
        String keyRefName = paramValueRef.substring("keyRef:".length()).trim();
        String secretValue = environment.getProperty(SECRET_PROPERTY_PREFIX + keyRefName);
        if (secretValue == null || secretValue.isBlank()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "未找到对应的密钥引用配置: " + keyRefName
            );
        }
        return secretValue;
    }
}
