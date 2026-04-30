package com.hjo2oa.org.role.resource.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleResourceAuthRepository {

    Optional<ResourceDefinition> findResourceById(UUID resourceId);

    Optional<ResourceDefinition> findResourceByCode(UUID tenantId, ResourceType resourceType, String resourceCode);

    List<ResourceDefinition> findResources(UUID tenantId, ResourceType resourceType, ResourceStatus status);

    ResourceDefinition saveResource(ResourceDefinition resource);

    void deleteResource(UUID resourceId);

    Optional<Role> findRoleById(UUID roleId);

    Optional<Role> findRoleByCodeAndTenantId(String code, UUID tenantId);

    List<Role> findRoles(UUID tenantId, RoleCategory category, RoleScope scope, RoleStatus status);

    List<Role> findActiveRolesByIds(UUID tenantId, Set<UUID> roleIds);

    Role saveRole(Role role);

    void deleteRole(UUID roleId);

    List<ResourcePermission> findResourcePermissions(UUID roleId);

    List<ResourcePermission> findResourcePermissionsByRoleIds(UUID tenantId, List<UUID> roleIds);

    List<ResourcePermission> replaceResourcePermissions(UUID roleId, List<ResourcePermission> permissions);

    boolean existsResourcePermission(UUID tenantId, ResourceType resourceType, String resourceCode, ResourceAction action);

    Optional<PersonRole> findPersonRole(UUID personId, UUID roleId);

    List<PersonRole> findPersonRolesByPerson(UUID personId, boolean includeExpired);

    List<PersonRole> findPersonRolesByPerson(UUID tenantId, UUID personId, boolean includeExpired);

    PersonRole savePersonRole(PersonRole personRole);

    void deletePersonRole(UUID personId, UUID roleId);

    Optional<PositionRoleGrant> findPositionRole(UUID tenantId, UUID positionId, UUID roleId);

    List<PositionRoleGrant> findPositionRoles(UUID tenantId, UUID positionId);

    PositionRoleGrant savePositionRole(PositionRoleGrant positionRole);

    void deletePositionRole(UUID tenantId, UUID positionId, UUID roleId);
}
