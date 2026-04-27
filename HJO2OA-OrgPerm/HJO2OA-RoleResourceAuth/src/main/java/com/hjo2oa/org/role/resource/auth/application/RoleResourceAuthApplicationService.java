package com.hjo2oa.org.role.resource.auth.application;

import com.hjo2oa.org.role.resource.auth.domain.PersonRole;
import com.hjo2oa.org.role.resource.auth.domain.PersonRoleView;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermission;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermissionView;
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
import java.util.Set;
import java.util.UUID;
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
    private final Clock clock;

    public RoleResourceAuthApplicationService(RoleResourceAuthRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public RoleResourceAuthApplicationService(RoleResourceAuthRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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
        return repository.saveRole(role.update(
                command.code(),
                command.name(),
                command.category(),
                command.scope(),
                command.description(),
                now()
        )).toView();
    }

    public RoleView changeRoleStatus(RoleResourceAuthCommands.ChangeRoleStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Role role = loadRequiredRole(command.roleId());
        Role updated = command.status() == RoleStatus.DISABLED ? role.disable(now()) : role.enable(now());
        return repository.saveRole(updated).toView();
    }

    public void deleteRole(UUID roleId) {
        loadRequiredRole(roleId);
        repository.deleteRole(roleId);
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
        return repository.replaceResourcePermissions(role.id(), permissions).stream()
                .sorted(PERMISSION_ORDER)
                .map(ResourcePermission::toView)
                .toList();
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
        return repository.savePersonRole(new PersonRole(
                personRoleId,
                command.personId(),
                command.roleId(),
                reason,
                command.expiresAt(),
                role.tenantId()
        )).toView();
    }

    public void revokePersonRole(UUID personId, UUID roleId) {
        repository.findPersonRole(personId, roleId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Direct person role grant not found"
                ));
        repository.deletePersonRole(personId, roleId);
    }

    public List<PersonRoleView> queryDirectPersonRoles(UUID personId, boolean includeExpired) {
        return repository.findPersonRolesByPerson(personId, includeExpired).stream()
                .map(PersonRole::toView)
                .toList();
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
