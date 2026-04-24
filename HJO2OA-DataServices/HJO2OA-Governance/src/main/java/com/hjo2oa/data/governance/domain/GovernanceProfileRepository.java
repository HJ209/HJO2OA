package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import java.util.List;
import java.util.Optional;

public interface GovernanceProfileRepository {

    Optional<GovernanceProfile> findByCode(String tenantId, String code);

    Optional<GovernanceProfile> findByTarget(String tenantId, GovernanceScopeType scopeType, String targetCode);

    List<GovernanceProfile> findByTenant(String tenantId);

    List<GovernanceProfile> findAllActive();

    GovernanceProfile save(GovernanceProfile profile);
}
