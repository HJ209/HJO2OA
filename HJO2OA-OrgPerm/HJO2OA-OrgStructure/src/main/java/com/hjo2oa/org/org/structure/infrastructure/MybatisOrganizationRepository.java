package com.hjo2oa.org.org.structure.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.structure.domain.OrgStatus;
import com.hjo2oa.org.org.structure.domain.Organization;
import com.hjo2oa.org.org.structure.domain.OrganizationRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisOrganizationRepository implements OrganizationRepository {

    private static final Comparator<Organization> TREE_ORDER = Comparator
            .comparingInt(Organization::level)
            .thenComparingInt(Organization::sortOrder)
            .thenComparing(Organization::code);

    private final OrganizationMapper organizationMapper;

    public MybatisOrganizationRepository(OrganizationMapper organizationMapper) {
        this.organizationMapper = Objects.requireNonNull(organizationMapper, "organizationMapper must not be null");
    }

    @Override
    public Optional<Organization> findById(UUID organizationId) {
        return Optional.ofNullable(organizationMapper.selectById(organizationId)).map(this::toDomain);
    }

    @Override
    public Optional<Organization> findByTenantIdAndCode(UUID tenantId, String code) {
        LambdaQueryWrapper<OrganizationEntity> wrapper = Wrappers.<OrganizationEntity>lambdaQuery()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .eq(OrganizationEntity::getCode, code);
        return Optional.ofNullable(organizationMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<Organization> findByTenantId(UUID tenantId) {
        LambdaQueryWrapper<OrganizationEntity> wrapper = Wrappers.<OrganizationEntity>lambdaQuery()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .orderByAsc(OrganizationEntity::getLevel)
                .orderByAsc(OrganizationEntity::getSortOrder)
                .orderByAsc(OrganizationEntity::getCode);
        return organizationMapper.selectList(wrapper).stream().map(this::toDomain).sorted(TREE_ORDER).toList();
    }

    @Override
    public List<Organization> findByParentId(UUID tenantId, UUID parentId) {
        LambdaQueryWrapper<OrganizationEntity> wrapper = Wrappers.<OrganizationEntity>lambdaQuery()
                .eq(OrganizationEntity::getTenantId, tenantId);
        if (parentId == null) {
            wrapper.isNull(OrganizationEntity::getParentId);
        } else {
            wrapper.eq(OrganizationEntity::getParentId, parentId);
        }
        wrapper.orderByAsc(OrganizationEntity::getSortOrder).orderByAsc(OrganizationEntity::getCode);
        return organizationMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Organization> findByPathPrefix(UUID tenantId, String pathPrefix) {
        LambdaQueryWrapper<OrganizationEntity> wrapper = Wrappers.<OrganizationEntity>lambdaQuery()
                .eq(OrganizationEntity::getTenantId, tenantId)
                .likeRight(OrganizationEntity::getPath, pathPrefix)
                .orderByAsc(OrganizationEntity::getLevel);
        return organizationMapper.selectList(wrapper).stream().map(this::toDomain).sorted(TREE_ORDER).toList();
    }

    @Override
    public Organization save(Organization organization) {
        OrganizationEntity existing = organizationMapper.selectById(organization.id());
        OrganizationEntity entity = toEntity(organization, existing);
        if (existing == null) {
            organizationMapper.insert(entity);
        } else {
            organizationMapper.updateById(entity);
        }
        return findById(organization.id()).orElseThrow();
    }

    @Override
    public List<Organization> saveAll(List<Organization> organizations) {
        return organizations.stream().map(this::save).toList();
    }

    @Override
    public void deleteById(UUID organizationId) {
        organizationMapper.deleteById(organizationId);
    }

    private Organization toDomain(OrganizationEntity entity) {
        return new Organization(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getShortName(),
                entity.getType(),
                entity.getParentId(),
                entity.getLevel() == null ? 0 : entity.getLevel(),
                entity.getPath(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                OrgStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrganizationEntity toEntity(Organization organization, OrganizationEntity existing) {
        OrganizationEntity entity = existing == null ? new OrganizationEntity() : existing;
        entity.setId(organization.id());
        entity.setCode(organization.code());
        entity.setName(organization.name());
        entity.setShortName(organization.shortName());
        entity.setType(organization.type());
        entity.setParentId(organization.parentId());
        entity.setLevel(organization.level());
        entity.setPath(organization.path());
        entity.setSortOrder(organization.sortOrder());
        entity.setStatus(organization.status().name());
        entity.setTenantId(organization.tenantId());
        entity.setCreatedAt(organization.createdAt());
        entity.setUpdatedAt(organization.updatedAt());
        return entity;
    }
}
