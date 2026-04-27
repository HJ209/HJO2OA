package com.hjo2oa.org.org.structure.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {

    Optional<Organization> findById(UUID organizationId);

    Optional<Organization> findByTenantIdAndCode(UUID tenantId, String code);

    List<Organization> findByTenantId(UUID tenantId);

    List<Organization> findByParentId(UUID tenantId, UUID parentId);

    List<Organization> findByPathPrefix(UUID tenantId, String pathPrefix);

    Organization save(Organization organization);

    List<Organization> saveAll(List<Organization> organizations);

    void deleteById(UUID organizationId);
}
