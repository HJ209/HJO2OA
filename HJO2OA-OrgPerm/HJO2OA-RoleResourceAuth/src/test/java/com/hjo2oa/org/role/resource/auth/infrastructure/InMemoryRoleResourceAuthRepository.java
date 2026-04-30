package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.hjo2oa.org.role.resource.auth.domain.PersonRole;
import com.hjo2oa.org.role.resource.auth.domain.PositionRoleGrant;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourceDefinition;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermission;
import com.hjo2oa.org.role.resource.auth.domain.ResourceStatus;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.Role;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleResourceAuthRepository;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class InMemoryRoleResourceAuthRepository implements RoleResourceAuthRepository {

    private final Map<UUID, ResourceDefinition> resourcesById = new LinkedHashMap<>();
    private final Map<UUID, Role> rolesById = new LinkedHashMap<>();
    private final Map<UUID, List<ResourcePermission>> permissionsByRoleId = new LinkedHashMap<>();
    private final Map<UUID, PersonRole> personRolesById = new LinkedHashMap<>();
    private final Map<UUID, PositionRoleGrant> positionRolesById = new LinkedHashMap<>();
    private final Clock clock;

    public InMemoryRoleResourceAuthRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<ResourceDefinition> findResourceById(UUID resourceId) {
        return Optional.ofNullable(resourcesById.get(resourceId));
    }

    @Override
    public Optional<ResourceDefinition> findResourceByCode(
            UUID tenantId,
            ResourceType resourceType,
            String resourceCode
    ) {
        return resourcesById.values().stream()
                .filter(resource -> resource.tenantId().equals(tenantId))
                .filter(resource -> resource.resourceType() == resourceType)
                .filter(resource -> resource.resourceCode().equals(resourceCode))
                .findFirst();
    }

    @Override
    public List<ResourceDefinition> findResources(UUID tenantId, ResourceType resourceType, ResourceStatus status) {
        return resourcesById.values().stream()
                .filter(resource -> tenantId == null || resource.tenantId().equals(tenantId))
                .filter(resource -> resourceType == null || resource.resourceType() == resourceType)
                .filter(resource -> status == null || resource.status() == status)
                .toList();
    }

    @Override
    public ResourceDefinition saveResource(ResourceDefinition resource) {
        resourcesById.put(resource.id(), resource);
        return resource;
    }

    @Override
    public void deleteResource(UUID resourceId) {
        resourcesById.remove(resourceId);
    }

    @Override
    public Optional<Role> findRoleById(UUID roleId) {
        return Optional.ofNullable(rolesById.get(roleId));
    }

    @Override
    public Optional<Role> findRoleByCodeAndTenantId(String code, UUID tenantId) {
        return rolesById.values().stream()
                .filter(role -> role.tenantId().equals(tenantId))
                .filter(role -> role.code().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public List<Role> findRoles(UUID tenantId, RoleCategory category, RoleScope scope, RoleStatus status) {
        return rolesById.values().stream()
                .filter(role -> tenantId == null || role.tenantId().equals(tenantId))
                .filter(role -> category == null || role.category() == category)
                .filter(role -> scope == null || role.scope() == scope)
                .filter(role -> status == null || role.status() == status)
                .toList();
    }

    @Override
    public List<Role> findActiveRolesByIds(UUID tenantId, Set<UUID> roleIds) {
        return rolesById.values().stream()
                .filter(role -> role.tenantId().equals(tenantId))
                .filter(role -> roleIds.contains(role.id()))
                .filter(role -> role.status() == RoleStatus.ACTIVE)
                .toList();
    }

    @Override
    public Role saveRole(Role role) {
        rolesById.put(role.id(), role);
        return role;
    }

    @Override
    public void deleteRole(UUID roleId) {
        rolesById.remove(roleId);
        permissionsByRoleId.remove(roleId);
        personRolesById.values().removeIf(personRole -> personRole.roleId().equals(roleId));
        positionRolesById.values().removeIf(positionRole -> positionRole.roleId().equals(roleId));
    }

    @Override
    public List<ResourcePermission> findResourcePermissions(UUID roleId) {
        return permissionsByRoleId.getOrDefault(roleId, List.of());
    }

    @Override
    public List<ResourcePermission> findResourcePermissionsByRoleIds(UUID tenantId, List<UUID> roleIds) {
        return roleIds.stream()
                .flatMap(roleId -> findResourcePermissions(roleId).stream())
                .filter(permission -> permission.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public List<ResourcePermission> replaceResourcePermissions(UUID roleId, List<ResourcePermission> permissions) {
        permissionsByRoleId.put(roleId, List.copyOf(permissions));
        return findResourcePermissions(roleId);
    }

    @Override
    public boolean existsResourcePermission(
            UUID tenantId,
            ResourceType resourceType,
            String resourceCode,
            ResourceAction action
    ) {
        return permissionsByRoleId.values().stream()
                .flatMap(List::stream)
                .anyMatch(permission -> permission.tenantId().equals(tenantId)
                        && permission.resourceType() == resourceType
                        && permission.resourceCode().equals(resourceCode)
                        && permission.action() == action);
    }

    @Override
    public Optional<PersonRole> findPersonRole(UUID personId, UUID roleId) {
        return personRolesById.values().stream()
                .filter(personRole -> personRole.personId().equals(personId))
                .filter(personRole -> personRole.roleId().equals(roleId))
                .findFirst();
    }

    @Override
    public List<PersonRole> findPersonRolesByPerson(UUID personId, boolean includeExpired) {
        return personRolesById.values().stream()
                .filter(personRole -> personRole.personId().equals(personId))
                .filter(personRole -> includeExpired || !personRole.expiredAt(clock.instant()))
                .toList();
    }

    @Override
    public List<PersonRole> findPersonRolesByPerson(UUID tenantId, UUID personId, boolean includeExpired) {
        return personRolesById.values().stream()
                .filter(personRole -> personRole.tenantId().equals(tenantId))
                .filter(personRole -> personRole.personId().equals(personId))
                .filter(personRole -> includeExpired || !personRole.expiredAt(clock.instant()))
                .toList();
    }

    @Override
    public PersonRole savePersonRole(PersonRole personRole) {
        personRolesById.put(personRole.id(), personRole);
        return personRole;
    }

    @Override
    public void deletePersonRole(UUID personId, UUID roleId) {
        personRolesById.values().removeIf(personRole -> personRole.personId().equals(personId)
                && personRole.roleId().equals(roleId));
    }

    @Override
    public Optional<PositionRoleGrant> findPositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        return positionRolesById.values().stream()
                .filter(positionRole -> positionRole.tenantId().equals(tenantId))
                .filter(positionRole -> positionRole.positionId().equals(positionId))
                .filter(positionRole -> positionRole.roleId().equals(roleId))
                .findFirst();
    }

    @Override
    public List<PositionRoleGrant> findPositionRoles(UUID tenantId, UUID positionId) {
        return positionRolesById.values().stream()
                .filter(positionRole -> positionRole.tenantId().equals(tenantId))
                .filter(positionRole -> positionRole.positionId().equals(positionId))
                .toList();
    }

    @Override
    public PositionRoleGrant savePositionRole(PositionRoleGrant positionRole) {
        positionRolesById.put(positionRole.id(), positionRole);
        return positionRole;
    }

    @Override
    public void deletePositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        positionRolesById.values().removeIf(positionRole -> positionRole.tenantId().equals(tenantId)
                && positionRole.positionId().equals(positionId)
                && positionRole.roleId().equals(roleId));
    }
}
