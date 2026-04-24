package com.hjo2oa.data.governance.infrastructure;

import com.hjo2oa.data.governance.domain.GovernanceProfile;
import com.hjo2oa.data.governance.domain.GovernanceProfileRepository;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import javax.sql.DataSource;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryGovernanceProfileRepository implements GovernanceProfileRepository {

    private final Map<String, GovernanceProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<GovernanceProfile> findByCode(String tenantId, String code) {
        return profiles.values().stream()
                .filter(profile -> profile.tenantId().equals(tenantId))
                .filter(profile -> profile.code().equals(code))
                .findFirst();
    }

    @Override
    public Optional<GovernanceProfile> findByTarget(String tenantId, GovernanceScopeType scopeType, String targetCode) {
        return profiles.values().stream()
                .filter(profile -> profile.tenantId().equals(tenantId))
                .filter(profile -> profile.scopeType() == scopeType)
                .filter(profile -> profile.targetCode().equals(targetCode))
                .findFirst();
    }

    @Override
    public java.util.List<GovernanceProfile> findByTenant(String tenantId) {
        return profiles.values().stream()
                .filter(profile -> profile.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(GovernanceProfile::code))
                .toList();
    }

    @Override
    public java.util.List<GovernanceProfile> findAllActive() {
        return profiles.values().stream()
                .filter(profile -> profile.status() == GovernanceProfileStatus.ACTIVE)
                .sorted(Comparator.comparing(GovernanceProfile::code))
                .toList();
    }

    @Override
    public GovernanceProfile save(GovernanceProfile profile) {
        profiles.put(profile.governanceId(), profile);
        return profile;
    }
}
