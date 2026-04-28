package com.hjo2oa.org.org.structure.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.org.structure.domain.Department;
import com.hjo2oa.org.org.structure.domain.DepartmentRepository;
import com.hjo2oa.org.org.structure.domain.DeptStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisDepartmentRepository implements DepartmentRepository {

    private static final Comparator<Department> TREE_ORDER = Comparator
            .comparingInt(Department::level)
            .thenComparingInt(Department::sortOrder)
            .thenComparing(Department::code);

    private final DepartmentMapper departmentMapper;

    public MybatisDepartmentRepository(DepartmentMapper departmentMapper) {
        this.departmentMapper = Objects.requireNonNull(departmentMapper, "departmentMapper must not be null");
    }

    @Override
    public Optional<Department> findById(UUID departmentId) {
        return Optional.ofNullable(departmentMapper.selectById(departmentId)).map(this::toDomain);
    }

    @Override
    public Optional<Department> findByTenantIdAndCode(UUID tenantId, String code) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = Wrappers.<DepartmentEntity>lambdaQuery()
                .eq(DepartmentEntity::getTenantId, tenantId)
                .eq(DepartmentEntity::getCode, code);
        return Optional.ofNullable(departmentMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<Department> findByOrganizationId(UUID tenantId, UUID organizationId) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = Wrappers.<DepartmentEntity>lambdaQuery()
                .eq(DepartmentEntity::getTenantId, tenantId)
                .eq(DepartmentEntity::getOrganizationId, organizationId)
                .orderByAsc(DepartmentEntity::getLevel)
                .orderByAsc(DepartmentEntity::getSortOrder)
                .orderByAsc(DepartmentEntity::getCode);
        return departmentMapper.selectList(wrapper).stream().map(this::toDomain).sorted(TREE_ORDER).toList();
    }

    @Override
    public List<Department> findByParentId(UUID tenantId, UUID organizationId, UUID parentId) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = Wrappers.<DepartmentEntity>lambdaQuery()
                .eq(DepartmentEntity::getTenantId, tenantId)
                .eq(DepartmentEntity::getOrganizationId, organizationId);
        if (parentId == null) {
            wrapper.isNull(DepartmentEntity::getParentId);
        } else {
            wrapper.eq(DepartmentEntity::getParentId, parentId);
        }
        wrapper.orderByAsc(DepartmentEntity::getSortOrder).orderByAsc(DepartmentEntity::getCode);
        return departmentMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Department> findByPathPrefix(UUID tenantId, UUID organizationId, String pathPrefix) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = Wrappers.<DepartmentEntity>lambdaQuery()
                .eq(DepartmentEntity::getTenantId, tenantId)
                .eq(DepartmentEntity::getOrganizationId, organizationId)
                .likeRight(DepartmentEntity::getPath, pathPrefix)
                .orderByAsc(DepartmentEntity::getLevel);
        return departmentMapper.selectList(wrapper).stream().map(this::toDomain).sorted(TREE_ORDER).toList();
    }

    @Override
    public Department save(Department department) {
        DepartmentEntity existing = departmentMapper.selectById(department.id());
        DepartmentEntity entity = toEntity(department, existing);
        if (existing == null) {
            departmentMapper.insert(entity);
        } else {
            departmentMapper.updateById(entity);
        }
        return findById(department.id()).orElseThrow();
    }

    @Override
    public List<Department> saveAll(List<Department> departments) {
        return departments.stream().map(this::save).toList();
    }

    @Override
    public void deleteById(UUID departmentId) {
        departmentMapper.deleteById(departmentId);
    }

    private Department toDomain(DepartmentEntity entity) {
        return new Department(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getOrganizationId(),
                entity.getParentId(),
                entity.getLevel() == null ? 0 : entity.getLevel(),
                entity.getPath(),
                entity.getManagerId(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                DeptStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DepartmentEntity toEntity(Department department, DepartmentEntity existing) {
        DepartmentEntity entity = existing == null ? new DepartmentEntity() : existing;
        entity.setId(department.id());
        entity.setCode(department.code());
        entity.setName(department.name());
        entity.setOrganizationId(department.organizationId());
        entity.setParentId(department.parentId());
        entity.setLevel(department.level());
        entity.setPath(department.path());
        entity.setManagerId(department.managerId());
        entity.setSortOrder(department.sortOrder());
        entity.setStatus(department.status().name());
        entity.setTenantId(department.tenantId());
        entity.setCreatedAt(department.createdAt());
        entity.setUpdatedAt(department.updatedAt());
        return entity;
    }
}
