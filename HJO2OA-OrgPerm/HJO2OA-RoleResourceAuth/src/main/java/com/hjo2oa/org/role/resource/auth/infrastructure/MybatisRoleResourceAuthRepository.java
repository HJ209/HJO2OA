package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.role.resource.auth.domain.PermissionEffect;
import com.hjo2oa.org.role.resource.auth.domain.PersonRole;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermission;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.Role;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleResourceAuthRepository;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisRoleResourceAuthRepository implements RoleResourceAuthRepository {

    private static final Comparator<ResourcePermission> PERMISSION_ORDER = Comparator
            .comparing(ResourcePermission::resourceType)
            .thenComparing(ResourcePermission::resourceCode)
            .thenComparing(ResourcePermission::action);

    private final RoleMapper roleMapper;
    private final ResourcePermissionMapper resourcePermissionMapper;
    private final PersonRoleMapper personRoleMapper;
    private final Clock clock;
    @Autowired
    public MybatisRoleResourceAuthRepository(
            RoleMapper roleMapper,
            ResourcePermissionMapper resourcePermissionMapper,
            PersonRoleMapper personRoleMapper
    ) {
        this(roleMapper, resourcePermissionMapper, personRoleMapper, Clock.systemUTC());
    }

    public MybatisRoleResourceAuthRepository(
            RoleMapper roleMapper,
            ResourcePermissionMapper resourcePermissionMapper,
            PersonRoleMapper personRoleMapper,
            Clock clock
    ) {
        this.roleMapper = Objects.requireNonNull(roleMapper, "roleMapper must not be null");
        this.resourcePermissionMapper = Objects.requireNonNull(
                resourcePermissionMapper,
                "resourcePermissionMapper must not be null"
        );
        this.personRoleMapper = Objects.requireNonNull(personRoleMapper, "personRoleMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<Role> findRoleById(UUID roleId) {
        return Optional.ofNullable(roleMapper.selectById(roleId)).map(this::toRole);
    }

    @Override
    public Optional<Role> findRoleByCodeAndTenantId(String code, UUID tenantId) {
        LambdaQueryWrapper<RoleEntity> wrapper = Wrappers.<RoleEntity>lambdaQuery()
                .eq(RoleEntity::getCode, Role.requireText(code, "code").toUpperCase(java.util.Locale.ROOT))
                .eq(RoleEntity::getTenantId, tenantId);
        return Optional.ofNullable(roleMapper.selectOne(wrapper)).map(this::toRole);
    }

    @Override
    public List<Role> findRoles(UUID tenantId, RoleCategory category, RoleScope scope, RoleStatus status) {
        LambdaQueryWrapper<RoleEntity> wrapper = Wrappers.<RoleEntity>lambdaQuery()
                .eq(tenantId != null, RoleEntity::getTenantId, tenantId)
                .eq(category != null, RoleEntity::getCategory, category == null ? null : category.name())
                .eq(scope != null, RoleEntity::getScope, scope == null ? null : scope.name())
                .eq(status != null, RoleEntity::getStatus, status == null ? null : status.name())
                .orderByAsc(RoleEntity::getCode);
        return roleMapper.selectList(wrapper).stream().map(this::toRole).toList();
    }

    @Override
    public Role saveRole(Role role) {
        RoleEntity existing = roleMapper.selectById(role.id());
        RoleEntity entity = toRoleEntity(role, existing);
        if (existing == null) {
            roleMapper.insert(entity);
        } else {
            roleMapper.updateById(entity);
        }
        return toRole(roleMapper.selectById(role.id()));
    }

    @Override
    public void deleteRole(UUID roleId) {
        resourcePermissionMapper.delete(Wrappers.<ResourcePermissionEntity>lambdaQuery()
                .eq(ResourcePermissionEntity::getRoleId, roleId));
        personRoleMapper.delete(Wrappers.<PersonRoleEntity>lambdaQuery()
                .eq(PersonRoleEntity::getRoleId, roleId));
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<ResourcePermission> findResourcePermissions(UUID roleId) {
        return resourcePermissionMapper.selectList(Wrappers.<ResourcePermissionEntity>lambdaQuery()
                        .eq(ResourcePermissionEntity::getRoleId, roleId)
                        .orderByAsc(ResourcePermissionEntity::getResourceType)
                        .orderByAsc(ResourcePermissionEntity::getResourceCode)
                        .orderByAsc(ResourcePermissionEntity::getAction))
                .stream()
                .map(this::toResourcePermission)
                .sorted(PERMISSION_ORDER)
                .toList();
    }

    @Override
    public List<ResourcePermission> replaceResourcePermissions(UUID roleId, List<ResourcePermission> permissions) {
        resourcePermissionMapper.delete(Wrappers.<ResourcePermissionEntity>lambdaQuery()
                .eq(ResourcePermissionEntity::getRoleId, roleId));
        for (ResourcePermission permission : permissions) {
            resourcePermissionMapper.insert(toResourcePermissionEntity(permission, null));
        }
        return findResourcePermissions(roleId);
    }

    @Override
    public Optional<PersonRole> findPersonRole(UUID personId, UUID roleId) {
        LambdaQueryWrapper<PersonRoleEntity> wrapper = Wrappers.<PersonRoleEntity>lambdaQuery()
                .eq(PersonRoleEntity::getPersonId, personId)
                .eq(PersonRoleEntity::getRoleId, roleId);
        return Optional.ofNullable(personRoleMapper.selectOne(wrapper)).map(this::toPersonRole);
    }

    @Override
    public List<PersonRole> findPersonRolesByPerson(UUID personId, boolean includeExpired) {
        LambdaQueryWrapper<PersonRoleEntity> wrapper = Wrappers.<PersonRoleEntity>lambdaQuery()
                .eq(PersonRoleEntity::getPersonId, personId)
                .orderByAsc(PersonRoleEntity::getRoleId);
        List<PersonRole> personRoles = personRoleMapper.selectList(wrapper).stream()
                .map(this::toPersonRole)
                .toList();
        if (includeExpired) {
            return personRoles;
        }
        return personRoles.stream()
                .filter(personRole -> !personRole.expiredAt(clock.instant()))
                .toList();
    }

    @Override
    public PersonRole savePersonRole(PersonRole personRole) {
        PersonRoleEntity existing = personRoleMapper.selectById(personRole.id());
        PersonRoleEntity entity = toPersonRoleEntity(personRole, existing);
        if (existing == null) {
            personRoleMapper.insert(entity);
        } else {
            personRoleMapper.updateById(entity);
        }
        return toPersonRole(personRoleMapper.selectById(personRole.id()));
    }

    @Override
    public void deletePersonRole(UUID personId, UUID roleId) {
        personRoleMapper.delete(Wrappers.<PersonRoleEntity>lambdaQuery()
                .eq(PersonRoleEntity::getPersonId, personId)
                .eq(PersonRoleEntity::getRoleId, roleId));
    }

    private Role toRole(RoleEntity entity) {
        return new Role(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                RoleCategory.valueOf(entity.getCategory()),
                RoleScope.valueOf(entity.getScope()),
                entity.getDescription(),
                RoleStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private RoleEntity toRoleEntity(Role role, RoleEntity existing) {
        RoleEntity entity = existing == null ? new RoleEntity() : existing;
        entity.setId(role.id());
        entity.setCode(role.code());
        entity.setName(role.name());
        entity.setCategory(role.category().name());
        entity.setScope(role.scope().name());
        entity.setDescription(role.description());
        entity.setStatus(role.status().name());
        entity.setTenantId(role.tenantId());
        entity.setCreatedAt(role.createdAt());
        entity.setUpdatedAt(role.updatedAt());
        return entity;
    }

    private ResourcePermission toResourcePermission(ResourcePermissionEntity entity) {
        return new ResourcePermission(
                entity.getId(),
                entity.getRoleId(),
                ResourceType.valueOf(entity.getResourceType()),
                entity.getResourceCode(),
                ResourceAction.valueOf(entity.getAction()),
                PermissionEffect.valueOf(entity.getEffect()),
                entity.getTenantId()
        );
    }

    private ResourcePermissionEntity toResourcePermissionEntity(
            ResourcePermission permission,
            ResourcePermissionEntity existing
    ) {
        ResourcePermissionEntity entity = existing == null ? new ResourcePermissionEntity() : existing;
        entity.setId(permission.id());
        entity.setRoleId(permission.roleId());
        entity.setResourceType(permission.resourceType().name());
        entity.setResourceCode(permission.resourceCode());
        entity.setAction(permission.action().name());
        entity.setEffect(permission.effect().name());
        entity.setTenantId(permission.tenantId());
        return entity;
    }

    private PersonRole toPersonRole(PersonRoleEntity entity) {
        return new PersonRole(
                entity.getId(),
                entity.getPersonId(),
                entity.getRoleId(),
                entity.getReason(),
                entity.getExpiresAt(),
                entity.getTenantId()
        );
    }

    private PersonRoleEntity toPersonRoleEntity(PersonRole personRole, PersonRoleEntity existing) {
        PersonRoleEntity entity = existing == null ? new PersonRoleEntity() : existing;
        entity.setId(personRole.id());
        entity.setPersonId(personRole.personId());
        entity.setRoleId(personRole.roleId());
        entity.setReason(personRole.reason());
        entity.setExpiresAt(personRole.expiresAt());
        entity.setTenantId(personRole.tenantId());
        return entity;
    }
}
