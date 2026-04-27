package com.hjo2oa.org.data.permission.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.data.permission.domain.DataPermission;
import com.hjo2oa.org.data.permission.domain.DataPermissionQuery;
import com.hjo2oa.org.data.permission.domain.DataPermissionRepository;
import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermission;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.FieldPermissionQuery;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisDataPermissionRepository implements DataPermissionRepository {

    private final DataPermissionMapper dataPermissionMapper;
    private final FieldPermissionMapper fieldPermissionMapper;

    public MybatisDataPermissionRepository(
            DataPermissionMapper dataPermissionMapper,
            FieldPermissionMapper fieldPermissionMapper
    ) {
        this.dataPermissionMapper = Objects.requireNonNull(dataPermissionMapper, "dataPermissionMapper must not be null");
        this.fieldPermissionMapper = Objects.requireNonNull(
                fieldPermissionMapper,
                "fieldPermissionMapper must not be null"
        );
    }

    @Override
    public Optional<DataPermission> findRowPolicyById(UUID policyId) {
        return Optional.ofNullable(dataPermissionMapper.selectById(policyId)).map(this::toDomain);
    }

    @Override
    public List<DataPermission> findRowPolicies(DataPermissionQuery query) {
        LambdaQueryWrapper<DataPermissionEntity> wrapper = Wrappers.<DataPermissionEntity>lambdaQuery()
                .orderByDesc(DataPermissionEntity::getPriority)
                .orderByAsc(DataPermissionEntity::getBusinessObject);
        if (query != null) {
            applyRowQuery(wrapper, query);
        }
        return dataPermissionMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public DataPermission saveRowPolicy(DataPermission policy) {
        DataPermissionEntity existing = dataPermissionMapper.selectById(policy.id());
        DataPermissionEntity entity = toEntity(policy, existing);
        if (existing == null) {
            dataPermissionMapper.insert(entity);
        } else {
            dataPermissionMapper.updateById(entity);
        }
        return findRowPolicyById(policy.id()).orElseThrow();
    }

    @Override
    public void deleteRowPolicy(UUID policyId) {
        dataPermissionMapper.deleteById(policyId);
    }

    @Override
    public Optional<FieldPermission> findFieldPolicyById(UUID policyId) {
        return Optional.ofNullable(fieldPermissionMapper.selectById(policyId)).map(this::toDomain);
    }

    @Override
    public List<FieldPermission> findFieldPolicies(FieldPermissionQuery query) {
        LambdaQueryWrapper<FieldPermissionEntity> wrapper = Wrappers.<FieldPermissionEntity>lambdaQuery()
                .orderByAsc(FieldPermissionEntity::getBusinessObject)
                .orderByAsc(FieldPermissionEntity::getUsageScenario)
                .orderByAsc(FieldPermissionEntity::getFieldCode);
        if (query != null) {
            applyFieldQuery(wrapper, query);
        }
        return fieldPermissionMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public FieldPermission saveFieldPolicy(FieldPermission policy) {
        FieldPermissionEntity existing = fieldPermissionMapper.selectById(policy.id());
        FieldPermissionEntity entity = toEntity(policy, existing);
        if (existing == null) {
            fieldPermissionMapper.insert(entity);
        } else {
            fieldPermissionMapper.updateById(entity);
        }
        return findFieldPolicyById(policy.id()).orElseThrow();
    }

    @Override
    public void deleteFieldPolicy(UUID policyId) {
        fieldPermissionMapper.deleteById(policyId);
    }

    private void applyRowQuery(LambdaQueryWrapper<DataPermissionEntity> wrapper, DataPermissionQuery query) {
        optionalEq(wrapper, query.subjectType(), DataPermissionEntity::getSubjectType);
        optionalEq(wrapper, query.subjectId(), DataPermissionEntity::getSubjectId);
        optionalEq(wrapper, query.businessObject(), DataPermissionEntity::getBusinessObject);
        optionalEq(wrapper, query.scopeType(), DataPermissionEntity::getScopeType);
        optionalEq(wrapper, query.effect(), DataPermissionEntity::getEffect);
        optionalTenant(wrapper, query.tenantId(), DataPermissionEntity::getTenantId);
    }

    private void applyFieldQuery(LambdaQueryWrapper<FieldPermissionEntity> wrapper, FieldPermissionQuery query) {
        optionalEq(wrapper, query.subjectType(), FieldPermissionEntity::getSubjectType);
        optionalEq(wrapper, query.subjectId(), FieldPermissionEntity::getSubjectId);
        optionalEq(wrapper, query.businessObject(), FieldPermissionEntity::getBusinessObject);
        optionalEq(wrapper, query.usageScenario(), FieldPermissionEntity::getUsageScenario);
        optionalEq(wrapper, query.fieldCode(), FieldPermissionEntity::getFieldCode);
        optionalEq(wrapper, query.action(), FieldPermissionEntity::getAction);
        optionalEq(wrapper, query.effect(), FieldPermissionEntity::getEffect);
        optionalTenant(wrapper, query.tenantId(), FieldPermissionEntity::getTenantId);
    }

    private <T> void optionalEq(
            LambdaQueryWrapper<T> wrapper,
            Object value,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column
    ) {
        if (value == null) {
            return;
        }
        wrapper.eq(column, value instanceof Enum<?> enumValue ? enumValue.name() : value);
    }

    private <T> void optionalTenant(
            LambdaQueryWrapper<T> wrapper,
            UUID tenantId,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column
    ) {
        if (tenantId == null) {
            return;
        }
        wrapper.eq(column, tenantId);
    }

    private DataPermissionEntity toEntity(DataPermission policy, DataPermissionEntity existing) {
        DataPermissionEntity entity = existing == null ? new DataPermissionEntity() : existing;
        entity.setId(policy.id());
        entity.setSubjectType(policy.subjectType().name());
        entity.setSubjectId(policy.subjectId());
        entity.setBusinessObject(policy.businessObject());
        entity.setScopeType(policy.scopeType().name());
        entity.setConditionExpr(policy.conditionExpr());
        entity.setEffect(policy.effect().name());
        entity.setPriority(policy.priority());
        entity.setTenantId(policy.tenantId());
        entity.setCreatedAt(policy.createdAt());
        entity.setUpdatedAt(policy.updatedAt());
        return entity;
    }

    private FieldPermissionEntity toEntity(FieldPermission policy, FieldPermissionEntity existing) {
        FieldPermissionEntity entity = existing == null ? new FieldPermissionEntity() : existing;
        entity.setId(policy.id());
        entity.setSubjectType(policy.subjectType().name());
        entity.setSubjectId(policy.subjectId());
        entity.setBusinessObject(policy.businessObject());
        entity.setUsageScenario(policy.usageScenario());
        entity.setFieldCode(policy.fieldCode());
        entity.setAction(policy.action().name());
        entity.setEffect(policy.effect().name());
        entity.setTenantId(policy.tenantId());
        entity.setCreatedAt(policy.createdAt());
        entity.setUpdatedAt(policy.updatedAt());
        return entity;
    }

    private DataPermission toDomain(DataPermissionEntity entity) {
        return new DataPermission(
                entity.getId(),
                PermissionSubjectType.valueOf(entity.getSubjectType()),
                entity.getSubjectId(),
                entity.getBusinessObject(),
                DataScopeType.valueOf(entity.getScopeType()),
                entity.getConditionExpr(),
                PermissionEffect.valueOf(entity.getEffect()),
                entity.getPriority() == null ? 0 : entity.getPriority(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private FieldPermission toDomain(FieldPermissionEntity entity) {
        return new FieldPermission(
                entity.getId(),
                PermissionSubjectType.valueOf(entity.getSubjectType()),
                entity.getSubjectId(),
                entity.getBusinessObject(),
                entity.getUsageScenario(),
                entity.getFieldCode(),
                FieldPermissionAction.valueOf(entity.getAction()),
                PermissionEffect.valueOf(entity.getEffect()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
