package com.hjo2oa.infra.security.infrastructure;

import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemorySecurityPolicyRepository implements SecurityPolicyRepository {

    private final Map<UUID, SecurityPolicy> policies = new ConcurrentHashMap<>();

    @Override
    public Optional<SecurityPolicy> findById(UUID id) {
        return Optional.ofNullable(policies.get(id));
    }

    @Override
    public Optional<SecurityPolicy> findByPolicyCode(String policyCode) {
        return policies.values().stream()
                .filter(policy -> policy.policyCode().equalsIgnoreCase(policyCode))
                .findFirst();
    }

    @Override
    public List<SecurityPolicy> findAll() {
        return policies.values().stream()
                .sorted(Comparator.comparing(SecurityPolicy::updatedAt).reversed())
                .toList();
    }

    @Override
    public SecurityPolicy save(SecurityPolicy securityPolicy) {
        policies.put(securityPolicy.id(), securityPolicy);
        return securityPolicy;
    }
}
