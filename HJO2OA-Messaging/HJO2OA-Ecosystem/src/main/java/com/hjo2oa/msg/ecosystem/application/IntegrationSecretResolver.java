package com.hjo2oa.msg.ecosystem.application;

import java.util.Optional;

public interface IntegrationSecretResolver {

    Optional<String> resolveSecret(String configRef);
}
