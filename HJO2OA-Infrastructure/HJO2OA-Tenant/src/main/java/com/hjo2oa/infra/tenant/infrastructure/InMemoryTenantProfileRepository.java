package com.hjo2oa.infra.tenant.infrastructure;

import com.hjo2oa.infra.tenant.domain.TenantProfile;
import com.hjo2oa.infra.tenant.domain.TenantProfileRepository;
import com.hjo2oa.infra.tenant.domain.TenantStatus;
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
public class InMemoryTenantProfileRepository implements TenantProfileRepository {

    private final Map<UUID, TenantProfile> profilesById = new ConcurrentHashMap<>();

    @Override
    public Optional<TenantProfile> findByCode(UUID tenantId, String code) {
        return profilesById.values().stream()
                .filter(profile -> profile.tenantCode().equals(code))
                .findFirst();
    }

    @Override
    public Optional<TenantProfile> findByTenantId(UUID tenantId) {
        return Optional.ofNullable(profilesById.get(tenantId));
    }

    @Override
    public TenantProfile save(TenantProfile profile) {
        profilesById.put(profile.id(), profile);
        return profile;
    }

    @Override
    public List<TenantProfile> findAll() {
        return List.copyOf(profilesById.values());
    }

    @Override
    public List<TenantProfile> findAllActive() {
        return profilesById.values().stream()
                .filter(profile -> profile.status() == TenantStatus.ACTIVE)
                .toList();
    }
}
