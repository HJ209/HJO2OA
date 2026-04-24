package com.hjo2oa.infra.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityPolicyRepository {

    Optional<SecurityPolicy> findById(UUID id);

    Optional<SecurityPolicy> findByPolicyCode(String policyCode);

    List<SecurityPolicy> findAll();

    SecurityPolicy save(SecurityPolicy securityPolicy);
}
