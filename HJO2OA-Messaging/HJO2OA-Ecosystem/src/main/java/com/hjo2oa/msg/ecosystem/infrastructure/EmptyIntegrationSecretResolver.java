package com.hjo2oa.msg.ecosystem.infrastructure;

import com.hjo2oa.msg.ecosystem.application.IntegrationSecretResolver;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EmptyIntegrationSecretResolver implements IntegrationSecretResolver {

    @Override
    public Optional<String> resolveSecret(String configRef) {
        return Optional.empty();
    }
}
