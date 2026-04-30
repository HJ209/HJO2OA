package com.hjo2oa.org.role.resource.auth.application;

import com.hjo2oa.org.role.resource.auth.domain.PermissionCacheInvalidator;
import com.hjo2oa.org.role.resource.auth.domain.PermissionDecisionView;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshot;
import com.hjo2oa.org.role.resource.auth.domain.PersonRole;
import com.hjo2oa.org.role.resource.auth.domain.PersonRoleView;
import com.hjo2oa.org.role.resource.auth.domain.PositionRoleGrant;
import com.hjo2oa.org.role.resource.auth.domain.PositionRoleGrantView;
import com.hjo2oa.org.role.resource.auth.domain.ResourceDefinition;
import com.hjo2oa.org.role.resource.auth.domain.ResourceDefinitionView;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermission;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermissionView;
import com.hjo2oa.org.role.resource.auth.domain.ResourceStatus;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.Role;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleResourceAuthRepository;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import com.hjo2oa.org.role.resource.auth.domain.RoleView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleResourceAuthApplicationService {

    private static final Comparator<Role> ROLE_ORDER = Comparator
            .comparing(Role::code)
            .thenComparing(Role::name);
    private static final Comparator<ResourcePermission> PERMISSION_ORDER = Comparator
            .comparing(ResourcePermission::resourceType)
            .thenComparing(ResourcePermission::resourceCode)
            .thenComparing(ResourcePermission::action);

    private final RoleResourceAuthRepository repository;
    private final PermissionCalculator permissionCalculator;
    private final PermissionCacheInvalidator cacheInvalidator;
    private final Clock clock;

    @Autowired
    public RoleResourceAuthApplicationService(
            RoleResourceAuthRepository repository,
            PermissionCalculator permissionCalculator,
            PermissionCacheInvalidator cacheInvalidator
    ) {
        this(repository, permissionCalculator, cacheInvalidator, Clock.systemUTC());
    }

    public RoleResourceAuthApplicationService(RoleResourceAuthRepository repository) {
        this(repository, null, PermissionCacheInvalidator.noop(), Clock.systemUTC());
    }

    public RoleResourceAuthApplicationService(
            RoleResourceAuthRepository repository,
            PermissionCalculator permissionCalculator,
            PermissionCacheInvalidator cacheInvalidator,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.permissionCalculator = permissionCalculator;
        this.cacheInvalidator = Objects.requireNonNull(cacheInvalidator, "cacheInvalidator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ResourceDefinitionView saveResource(RoleResourceAuthCommands.SaveResourceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Optional<ResourceDefinition> existing = repository.findResourceByCode(
                command.tenantId(),
                command.resourceType(),
                command.resourceCode()
        );
        Instant now = now();
        ResourceDefinition resource = existing.map(current -> current.update(
                        command.resourceType(),
                        command.resourceCode(),
                        command.name(),
                        command.parentCode(),
                        command.sortOrder(),
                        command.status(),
                        now
                ))
                .orElseGet(() -> ResourceDefinition.create(
                        UUID.randomUUID(),
                        command.resourceType(),
                        command.resourceCode(),
                        command.name(),
                        command.parentCode(),
                        command.sortOrder(),
                        command.tenantId(),
                        now
                ));
        return repository.saveResource(resource).toView();
    }

    public void deleteResource(UUID resourceId) {
        ResourceDefinition resource = repository.findResourceById(resourceId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Resource not found"
                ));
        repository.deleteResource(resource.id());
        cacheInvalidator.invalidateTenant(resource.tenantId());
    }

    public List<ResourceDefinitionView> queryResources(
            UUID tenantId,
            ResourceType resourceType,
            ResourceStatus status
    ) {
        return repository.findResources(tenantId, resourceType, status).stream()
                .map(ResourceDefinition::toView)
                .toList();
    }

    public RoleView createRole(RoleResourceAuthCommands.CreateRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureRoleCodeUnique(command.code(), command.tenantId(), null);
        Role role = Role.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.category(),
                command.scope(),
                command.description(),
                command.tenantId(),
                now()
        );
        return repository.saveRole(role).toView();
    }

    public RoleView updateRole(RoleResourceAuthCommands.UpdateRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Role role = loadRequiredRole(command.roleId());
        ensureRoleCodeUnique(command.code(), role.tenantId(), role.id());
        RoleView view = repository.saveRole(role.update(
                command.code(),
                command.name(),
                command.category(),
                command.scope(),
                command.description(),
                now()
        )).toView();
        cacheInvalidator.invalidateRole(role.tenantId(), role.id());
        return view;
    }

    public RoleView changeRoleStatus(RoleResourceAuthCommands.ChangeRoleStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Role role = loadRequiredRole(command.roleId());
        Role updated = command.status() == RoleStatus.DISABLED ? role.disable(now()) : role.enable(now());
        RoleView view = repository.saveRole(updated).toView();
        cacheInvalidator.invalidateRole(role.tenantId(), role.id());
        return view;
    }

    public void deleteRole(UUID roleId) {
        Role role = loadRequiredRole(roleId);
        repository.deleteRole(roleId);
        cacheInvalidator.invalidateRole(role.tenantId(), role.id());
    }

    public RoleView getRole(UUID roleId) {
        return loadRequiredRole(roleId).toView();
    }

    public List<RoleView> queryRoles(UUID tenantId, RoleCategory category, RoleScope scope, RoleStatus status) {
        return repository.findRoles(tenantId, category, scope, status).stream()
                .sorted(ROLE_ORDER)
                .map(Role::toView)
                .toList();
    }

    public List<ResourcePermissionView> replaceResourcePermissions(
            RoleResourceAuthCommands.ReplaceResourcePermissionsCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        Role role = loadRequiredActiveRole(command.roleId());
        List<RoleResourceAuthCommands.ResourcePermissionItem> items =
                command.permissions() == null ? List.of() : command.permissions();
        ensureNoDuplicatePermissions(items);
        List<ResourcePermission> permissions = items.stream()
                .map(item -> ResourcePermission.create(
                        UUID.randomUUID(),
                        role.id(),
                        item.resourceType(),
                        item.resourceCode(),
                        item.action(),
                        item.effect(),
                        role.tenantId()
                ))
                .toList();
        List<ResourcePermissionView> views = repository.replaceResourcePermissions(role.id(), permissions).stream()
                .sorted(PERMISSION_ORDER)
                .map(ResourcePermission::toView)
                .toList();
        cacheInvalidator.invalidateRole(role.tenantId(), role.id());
        return views;
    }

    public List<ResourcePermissionView> queryResourcePermissions(UUID roleId) {
        loadRequiredRole(roleId);
        return repository.findResourcePermissions(roleId).stream()
                .sorted(PERMISSION_ORDER)
                .map(ResourcePermission::toView)
                .toList();
    }

    public PersonRoleView grantPersonRole(RoleResourceAuthCommands.GrantPersonRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Role role = loadRequiredActiveRole(command.roleId());
        String reason = Role.requireText(command.reason(), "reason");
        Optional<PersonRole> existing = repository.findPersonRole(command.personId(), command.roleId());
        if (existing.isPresent() && !existing.get().expiredAt(now())) {
            return existing.get().toView();
        }
        UUID personRoleId = existing.map(PersonRole::id).orElseGet(UUID::randomUUID);
        PersonRoleView view = repository.savePersonRole(new PersonRole(
                personRoleId,
                command.personId(),
                command.roleId(),
                reason,
                command.expiresAt(),
                role.tenantId()
        )).toView();
        cacheInvalidator.invalidatePerson(role.tenantId(), command.personId());
        return view;
    }

    public void revokePersonRole(UUID personId, UUID roleId) {
        PersonRole personRole = repository.findPersonRole(personId, roleId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Direct person role grant not found"
                ));
        repository.deletePersonRole(personId, roleId);
        cacheInvalidator.invalidatePerson(personRole.tenantId(), personId);
    }

    public List<PersonRoleView> queryDirectPersonRoles(UUID personId, boolean includeExpired) {
        return repository.findPersonRolesByPerson(personId, includeExpired).stream()
                .map(PersonRole::toView)
                .toList();
    }

    public List<PositionRoleGrantView> bindPositionRoles(
            RoleResourceAuthCommands.BindPositionRolesCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.roleIds() == null || command.roleIds().isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "roleIds must not be empty");
        }
        for (UUID roleId : command.roleIds()) {
            Role role = loadRequiredActiveRole(roleId);
            if (!role.tenantId().equals(command.tenantId())) {
                throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Role tenant mismatch");
            }
            repository.findPositionRole(command.tenantId(), command.positionId(), role.id())
                    .orElseGet(() -> repository.savePositionRole(PositionRoleGrant.create(
                            command.positionId(),
                            role.id(),
                            command.tenantId(),
                            now()
                    )));
        }
        cacheInvalidator.invalidatePosition(command.tenantId(), command.positionId());
        return queryPositionRoles(command.tenantId(), command.positionId());
    }

    public void unbindPositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        repository.deletePositionRole(tenantId, positionId, roleId);
        cacheInvalidator.invalidatePosition(tenantId, positionId);
    }

    public List<PositionRoleGrantView> queryPositionRoles(UUID tenantId, UUID positionId) {
        return repository.findPositionRoles(tenantId, positionId).stream()
                .map(PositionRoleGrant::toView)
                .toList();
    }

    public PermissionSnapshot calculatePermissions(UUID tenantId, UUID personId, UUID positionId) {
        return requirePermissionCalculator().calculate(tenantId, personId, positionId);
    }

    public PermissionDecisionView decideResource(RoleResourceAuthCommands.ResourceDecisionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return requirePermissionCalculator().decide(
                query.tenantId(),
                query.personId(),
                query.positionId(),
                query.resourceType(),
                query.resourceCode(),
                query.action()
        );
    }

    private PermissionCalculator requirePermissionCalculator() {
        if (permissionCalculator == null) {
            throw new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "Permission calculator is unavailable");
        }
        return permissionCalculator;
    }

    private Role loadRequiredActiveRole(UUID roleId) {
        Role role = loadRequiredRole(roleId);
        if (role.status() != RoleStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Role is disabled");
        }
        return role;
    }

    private Role loadRequiredRole(UUID roleId) {
        return repository.findRoleById(roleId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Role not found"));
    }

    private void ensureRoleCodeUnique(String code, UUID tenantId, UUID currentRoleId) {
        repository.findRoleByCodeAndTenantId(code, tenantId)
                .filter(existing -> !existing.id().equals(currentRoleId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Role code already exists");
                });
    }

    private void ensureNoDuplicatePermissions(List<RoleResourceAuthCommands.ResourcePermissionItem> permissions) {
        Set<String> keys = new HashSet<>();
        for (RoleResourceAuthCommands.ResourcePermissionItem item : permissions) {
            String key = item.resourceType() + "|" + item.resourceCode() + "|" + item.action();
            if (!keys.add(key)) {
                throw new BizException(SharedErrorDescriptors.CONFLICT, "Resource permission already exists");
            }
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
