package com.hjo2oa.org.role.resource.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleResourceAuthRepository {

    Optional<Role> findRoleById(UUID roleId);

    Optional<Role> findRoleByCodeAndTenantId(String code, UUID tenantId);

    List<Role> findRoles(UUID tenantId, RoleCategory category, RoleScope scope, RoleStatus status);

    Role saveRole(Role role);

    void deleteRole(UUID roleId);

    List<ResourcePermission> findResourcePermissions(UUID roleId);

    List<ResourcePermission> replaceResourcePermissions(UUID roleId, List<ResourcePermission> permissions);

    Optional<PersonRole> findPersonRole(UUID personId, UUID roleId);

    List<PersonRole> findPersonRolesByPerson(UUID personId, boolean includeExpired);

    PersonRole savePersonRole(PersonRole personRole);

    void deletePersonRole(UUID personId, UUID roleId);
}
