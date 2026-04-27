package com.hjo2oa.org.org.structure.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {

    Optional<Department> findById(UUID departmentId);

    Optional<Department> findByTenantIdAndCode(UUID tenantId, String code);

    List<Department> findByOrganizationId(UUID tenantId, UUID organizationId);

    List<Department> findByParentId(UUID tenantId, UUID organizationId, UUID parentId);

    List<Department> findByPathPrefix(UUID tenantId, UUID organizationId, String pathPrefix);

    Department save(Department department);

    List<Department> saveAll(List<Department> departments);

    void deleteById(UUID departmentId);
}
