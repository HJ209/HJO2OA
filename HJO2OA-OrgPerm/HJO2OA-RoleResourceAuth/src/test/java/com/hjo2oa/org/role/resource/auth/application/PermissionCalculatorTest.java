package com.hjo2oa.org.role.resource.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.org.role.resource.auth.domain.PermissionDecisionView;
import com.hjo2oa.org.role.resource.auth.domain.PermissionEffect;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshot;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import com.hjo2oa.org.role.resource.auth.infrastructure.InMemoryPermissionSnapshotCache;
import com.hjo2oa.org.role.resource.auth.infrastructure.InMemoryRoleResourceAuthRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-04-29T00:00:00Z");
    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERSON = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID POSITION = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void shouldMergePositionAndPersonRolesIntoFinalPermissions() {
        Fixture fixture = new Fixture();
        UUID positionRoleId = fixture.createRole("POSITION_ROLE");
        UUID personRoleId = fixture.createRole("PERSON_ROLE");
        fixture.bindPositionRole(POSITION, positionRoleId);
        fixture.grantPersonRole(PERSON, personRoleId);
        fixture.replace(positionRoleId, ResourceType.MENU, "portal.home", ResourceAction.READ, PermissionEffect.ALLOW);
        fixture.replace(personRoleId, ResourceType.BUTTON, "portal.approve", ResourceAction.UPDATE, PermissionEffect.ALLOW);

        PermissionSnapshot snapshot = fixture.calculator.calculate(TENANT, PERSON, POSITION);

        assertThat(snapshot.roleIds()).containsExactly(positionRoleId, personRoleId);
        assertThat(snapshot.resourcePermissions())
                .extracting(permission -> permission.resourceType() + ":" + permission.resourceCode())
                .containsExactly("MENU:portal.home", "BUTTON:portal.approve");
    }

    @Test
    void shouldDenyWhenPermissionMissingOrTenantDoesNotMatch() {
        Fixture fixture = new Fixture();
        UUID roleId = fixture.createRole("ROLE_A");
        fixture.bindPositionRole(POSITION, roleId);
        fixture.replace(roleId, ResourceType.API, "/api/v1/org/roles", ResourceAction.READ, PermissionEffect.ALLOW);

        PermissionDecisionView missing = fixture.calculator.decide(
                TENANT,
                PERSON,
                POSITION,
                ResourceType.API,
                "/api/v1/org/roles",
                ResourceAction.UPDATE
        );
        PermissionDecisionView crossTenant = fixture.calculator.decide(
                OTHER_TENANT,
                PERSON,
                POSITION,
                ResourceType.API,
                "/api/v1/org/roles",
                ResourceAction.READ
        );

        assertThat(missing.allowed()).isFalse();
        assertThat(crossTenant.allowed()).isFalse();
    }

    @Test
    void shouldTreatAncestorApiWildcardAsConfigured() {
        Fixture fixture = new Fixture();
        UUID roleId = fixture.createRole("ROLE_A");
        fixture.bindPositionRole(POSITION, roleId);
        fixture.replace(roleId, ResourceType.API, "/api/**", ResourceAction.READ, PermissionEffect.ALLOW);

        assertThat(fixture.calculator.isApiResourceConfigured(
                TENANT,
                "/api/v1/org/identity-context/current",
                ResourceAction.READ
        )).isTrue();
        assertThat(fixture.calculator.hasApiPermission(
                TENANT,
                PERSON,
                POSITION,
                "/api/v1/org/identity-context/current",
                ResourceAction.READ
        )).isTrue();
    }

    @Test
    void shouldInvalidateCacheAfterRoleChanges() {
        Fixture fixture = new Fixture();
        UUID roleId = fixture.createRole("ROLE_A");
        fixture.bindPositionRole(POSITION, roleId);
        fixture.replace(roleId, ResourceType.API, "/api/v1/org/roles", ResourceAction.READ, PermissionEffect.ALLOW);
        assertThat(fixture.calculator.hasApiPermission(
                TENANT,
                PERSON,
                POSITION,
                "/api/v1/org/roles",
                ResourceAction.READ
        )).isTrue();

        fixture.service.changeRoleStatus(new RoleResourceAuthCommands.ChangeRoleStatusCommand(roleId, RoleStatus.DISABLED));

        assertThat(fixture.calculator.hasApiPermission(
                TENANT,
                PERSON,
                POSITION,
                "/api/v1/org/roles",
                ResourceAction.READ
        )).isFalse();
    }

    @Test
    void shouldLetExplicitDenyOverrideAllow() {
        Fixture fixture = new Fixture();
        UUID allowRole = fixture.createRole("ALLOW_ROLE");
        UUID denyRole = fixture.createRole("DENY_ROLE");
        fixture.bindPositionRole(POSITION, allowRole);
        fixture.grantPersonRole(PERSON, denyRole);
        fixture.replace(allowRole, ResourceType.API, "/api/v1/org/roles", ResourceAction.READ, PermissionEffect.ALLOW);
        fixture.replace(denyRole, ResourceType.API, "/api/v1/org/roles", ResourceAction.READ, PermissionEffect.DENY);

        PermissionDecisionView decision = fixture.calculator.decide(
                TENANT,
                PERSON,
                POSITION,
                ResourceType.API,
                "/api/v1/org/roles",
                ResourceAction.READ
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.effect()).isEqualTo(PermissionEffect.DENY);
        assertThat(decision.matchedPermissions()).hasSize(2);
    }

    private static final class Fixture {

        private final InMemoryRoleResourceAuthRepository repository =
                new InMemoryRoleResourceAuthRepository(Clock.fixed(NOW, ZoneOffset.UTC));
        private final InMemoryPermissionSnapshotCache cache = new InMemoryPermissionSnapshotCache();
        private final PermissionCalculator calculator = new PermissionCalculator(repository, cache);
        private final RoleResourceAuthApplicationService service = new RoleResourceAuthApplicationService(
                repository,
                calculator,
                cache,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        private UUID createRole(String code) {
            return service.createRole(new RoleResourceAuthCommands.CreateRoleCommand(
                    code,
                    code,
                    RoleCategory.BUSINESS,
                    RoleScope.ORGANIZATION,
                    null,
                    TENANT
            )).id();
        }

        private void bindPositionRole(UUID positionId, UUID roleId) {
            service.bindPositionRoles(new RoleResourceAuthCommands.BindPositionRolesCommand(
                    TENANT,
                    positionId,
                    List.of(roleId),
                    "test"
            ));
        }

        private void grantPersonRole(UUID personId, UUID roleId) {
            service.grantPersonRole(new RoleResourceAuthCommands.GrantPersonRoleCommand(
                    personId,
                    roleId,
                    "test",
                    null
            ));
        }

        private void replace(
                UUID roleId,
                ResourceType resourceType,
                String resourceCode,
                ResourceAction action,
                PermissionEffect effect
        ) {
            service.replaceResourcePermissions(new RoleResourceAuthCommands.ReplaceResourcePermissionsCommand(
                    roleId,
                    List.of(new RoleResourceAuthCommands.ResourcePermissionItem(
                            resourceType,
                            resourceCode,
                            action,
                            effect
                    ))
            ));
        }
    }
}
