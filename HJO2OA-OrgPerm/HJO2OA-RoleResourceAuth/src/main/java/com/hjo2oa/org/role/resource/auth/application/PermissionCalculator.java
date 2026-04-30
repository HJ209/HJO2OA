package com.hjo2oa.org.role.resource.auth.application;

import com.hjo2oa.org.role.resource.auth.domain.PermissionCacheKey;
import com.hjo2oa.org.role.resource.auth.domain.PermissionDecisionView;
import com.hjo2oa.org.role.resource.auth.domain.PermissionEffect;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshot;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshotCache;
import com.hjo2oa.org.role.resource.auth.domain.PersonRole;
import com.hjo2oa.org.role.resource.auth.domain.PositionRoleGrant;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermissionView;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.Role;
import com.hjo2oa.org.role.resource.auth.domain.RoleResourceAuthRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PermissionCalculator {

    private final RoleResourceAuthRepository repository;
    private final PermissionSnapshotCache snapshotCache;

    public PermissionCalculator(
            RoleResourceAuthRepository repository,
            PermissionSnapshotCache snapshotCache
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.snapshotCache = Objects.requireNonNull(snapshotCache, "snapshotCache must not be null");
    }

    public PermissionSnapshot calculate(UUID tenantId, UUID personId, UUID positionId) {
        PermissionCacheKey key = new PermissionCacheKey(tenantId, personId, positionId);
        return snapshotCache.get(key).orElseGet(() -> snapshotCache.put(key, calculateUncached(key)));
    }

    public PermissionDecisionView decide(
            UUID tenantId,
            UUID personId,
            UUID positionId,
            ResourceType resourceType,
            String resourceCode,
            ResourceAction action
    ) {
        PermissionSnapshot snapshot = calculate(tenantId, personId, positionId);
        List<ResourcePermissionView> matched = snapshot.resourcePermissions().stream()
                .filter(permission -> permission.resourceType() == resourceType)
                .filter(permission -> resourceCodeMatches(permission.resourceCode(), resourceCode))
                .filter(permission -> permission.action() == action)
                .toList();
        boolean denied = matched.stream().anyMatch(permission -> permission.effect() == PermissionEffect.DENY);
        boolean allowed = !denied && matched.stream().anyMatch(permission -> permission.effect() == PermissionEffect.ALLOW);
        PermissionEffect effect = denied || !allowed ? PermissionEffect.DENY : PermissionEffect.ALLOW;
        return new PermissionDecisionView(resourceType, resourceCode, action, allowed, effect, matched, snapshot);
    }

    public boolean hasApiPermission(
            UUID tenantId,
            UUID personId,
            UUID positionId,
            String resourceCode,
            ResourceAction action
    ) {
        return decide(tenantId, personId, positionId, ResourceType.API, resourceCode, action).allowed();
    }

    public boolean isApiResourceConfigured(UUID tenantId, String resourceCode, ResourceAction action) {
        if (repository.existsResourcePermission(tenantId, ResourceType.API, resourceCode, action)) {
            return true;
        }
        for (String wildcard : wildcardCandidates(resourceCode)) {
            if (repository.existsResourcePermission(tenantId, ResourceType.API, wildcard, action)) {
                return true;
            }
        }
        return false;
    }

    private PermissionSnapshot calculateUncached(PermissionCacheKey key) {
        Set<UUID> candidateRoleIds = new LinkedHashSet<>();
        repository.findPositionRoles(key.tenantId(), key.positionId()).stream()
                .map(PositionRoleGrant::roleId)
                .forEach(candidateRoleIds::add);
        repository.findPersonRolesByPerson(key.tenantId(), key.personId(), false).stream()
                .map(PersonRole::roleId)
                .forEach(candidateRoleIds::add);

        List<UUID> activeRoleIds = repository.findActiveRolesByIds(key.tenantId(), candidateRoleIds).stream()
                .map(Role::id)
                .distinct()
                .toList();
        List<ResourcePermissionView> permissions = repository
                .findResourcePermissionsByRoleIds(key.tenantId(), activeRoleIds)
                .stream()
                .map(permission -> permission.toView())
                .toList();
        return new PermissionSnapshot(
                key.tenantId(),
                key.personId(),
                key.positionId(),
                activeRoleIds,
                permissions,
                snapshotCache.version(key)
        );
    }

    private boolean resourceCodeMatches(String configuredCode, String requestedCode) {
        if (configuredCode.equals(requestedCode)) {
            return true;
        }
        if (!configuredCode.endsWith("/**")) {
            return false;
        }
        String prefix = configuredCode.substring(0, configuredCode.length() - 3);
        return requestedCode.equals(prefix) || requestedCode.startsWith(prefix + "/");
    }

    private List<String> wildcardCandidates(String resourceCode) {
        Set<String> candidates = new LinkedHashSet<>();
        int slash = resourceCode.indexOf('/', 1);
        while (slash > 0) {
            candidates.add(resourceCode.substring(0, slash) + "/**");
            slash = resourceCode.indexOf('/', slash + 1);
        }
        int lastSlash = resourceCode.lastIndexOf('/');
        if (lastSlash > 0) {
            candidates.add(resourceCode.substring(0, lastSlash) + "/**");
        }
        return List.copyOf(candidates);
    }
}
