package com.hjo2oa.data.service.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@ConditionalOnBean(DataSource.class)
public class MybatisDataServiceDefinitionRepository implements DataServiceDefinitionRepository {

    private final DataServiceDefinitionMapper definitionMapper;
    private final ServiceParameterDefinitionMapper parameterMapper;
    private final ServiceFieldMappingMapper fieldMappingMapper;
    private final DataServiceJsonCodec jsonCodec;

    public MybatisDataServiceDefinitionRepository(
            DataServiceDefinitionMapper definitionMapper,
            ServiceParameterDefinitionMapper parameterMapper,
            ServiceFieldMappingMapper fieldMappingMapper,
            DataServiceJsonCodec jsonCodec
    ) {
        this.definitionMapper = Objects.requireNonNull(definitionMapper, "definitionMapper must not be null");
        this.parameterMapper = Objects.requireNonNull(parameterMapper, "parameterMapper must not be null");
        this.fieldMappingMapper = Objects.requireNonNull(fieldMappingMapper, "fieldMappingMapper must not be null");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec must not be null");
    }

    @Override
    public SearchResult<DataServiceDefinition> search(
            UUID tenantId,
            String code,
            String keyword,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.Status status,
            int page,
            int size
    ) {
        LambdaQueryWrapper<DataServiceDefinitionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DataServiceDefinitionEntity::getTenantId, tenantId);
        if (code != null && !code.isBlank()) {
            wrapper.like(DataServiceDefinitionEntity::getCode, code.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(criteria -> criteria.like(DataServiceDefinitionEntity::getCode, normalized)
                    .or()
                    .like(DataServiceDefinitionEntity::getName, normalized));
        }
        if (serviceType != null) {
            wrapper.eq(DataServiceDefinitionEntity::getServiceType, serviceType.name());
        }
        if (status != null) {
            wrapper.eq(DataServiceDefinitionEntity::getStatus, status.name());
        }
        wrapper.orderByDesc(DataServiceDefinitionEntity::getUpdatedAt)
                .orderByAsc(DataServiceDefinitionEntity::getCode);
        Page<DataServiceDefinitionEntity> pageRequest = Page.of(page, size);
        Page<DataServiceDefinitionEntity> result = definitionMapper.selectPage(pageRequest, wrapper);
        List<DataServiceDefinition> definitions = result.getRecords().stream()
                .map(entity -> toAggregate(entity, List.of(), List.of()))
                .toList();
        return new SearchResult<>(definitions, result.getTotal());
    }

    @Override
    public List<DataServiceDefinition> findAllActiveByTenant(UUID tenantId) {
        LambdaQueryWrapper<DataServiceDefinitionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DataServiceDefinitionEntity::getTenantId, tenantId)
                .eq(DataServiceDefinitionEntity::getStatus, DataServiceDefinition.Status.ACTIVE.name())
                .orderByAsc(DataServiceDefinitionEntity::getCode);
        return definitionMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .toList();
    }

    @Override
    public java.util.Optional<DataServiceDefinition> findById(UUID serviceId) {
        DataServiceDefinitionEntity entity = definitionMapper.selectById(serviceId);
        if (entity == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public java.util.Optional<DataServiceDefinition> findByCode(UUID tenantId, String code) {
        LambdaQueryWrapper<DataServiceDefinitionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DataServiceDefinitionEntity::getTenantId, tenantId)
                .eq(DataServiceDefinitionEntity::getCode, code)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        DataServiceDefinitionEntity entity = definitionMapper.selectOne(wrapper);
        if (entity == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public java.util.Optional<DataServiceDefinition> findActiveByCode(UUID tenantId, String code) {
        LambdaQueryWrapper<DataServiceDefinitionEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DataServiceDefinitionEntity::getTenantId, tenantId)
                .eq(DataServiceDefinitionEntity::getCode, code)
                .eq(DataServiceDefinitionEntity::getStatus, DataServiceDefinition.Status.ACTIVE.name())
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        DataServiceDefinitionEntity entity = definitionMapper.selectOne(wrapper);
        if (entity == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public DataServiceDefinition save(DataServiceDefinition definition) {
        DataServiceDefinitionEntity existingDefinition = definitionMapper.selectById(definition.serviceId());
        DataServiceDefinitionEntity entity = toEntity(definition, existingDefinition);
        if (existingDefinition == null) {
            definitionMapper.insert(entity);
        } else {
            definitionMapper.updateById(entity);
        }
        syncParameters(definition);
        syncFieldMappings(definition);
        return loadAggregate(definition.serviceId());
    }

    @Override
    public void delete(UUID serviceId) {
        definitionMapper.deleteById(serviceId);
        parameterMapper.delete(Wrappers.<ServiceParameterDefinitionEntity>lambdaQuery()
                .eq(ServiceParameterDefinitionEntity::getServiceId, serviceId));
        fieldMappingMapper.delete(Wrappers.<ServiceFieldMappingEntity>lambdaQuery()
                .eq(ServiceFieldMappingEntity::getServiceId, serviceId));
    }

    private DataServiceDefinition loadAggregate(UUID serviceId) {
        DataServiceDefinitionEntity entity = definitionMapper.selectById(serviceId);
        if (entity == null) {
            throw new IllegalStateException("DataServiceDefinitionEntity not found: " + serviceId);
        }
        List<ServiceParameterDefinitionEntity> parameterEntities = parameterMapper.selectList(
                Wrappers.<ServiceParameterDefinitionEntity>lambdaQuery()
                        .eq(ServiceParameterDefinitionEntity::getServiceId, serviceId)
                        .orderByAsc(ServiceParameterDefinitionEntity::getSortOrder)
                        .orderByAsc(ServiceParameterDefinitionEntity::getParamCode)
        );
        List<ServiceFieldMappingEntity> fieldMappingEntities = fieldMappingMapper.selectList(
                Wrappers.<ServiceFieldMappingEntity>lambdaQuery()
                        .eq(ServiceFieldMappingEntity::getServiceId, serviceId)
                        .orderByAsc(ServiceFieldMappingEntity::getSortOrder)
                        .orderByAsc(ServiceFieldMappingEntity::getTargetField)
                        .orderByAsc(ServiceFieldMappingEntity::getSourceField)
        );
        return toAggregate(entity, parameterEntities, fieldMappingEntities);
    }

    private void syncParameters(DataServiceDefinition definition) {
        List<ServiceParameterDefinitionEntity> existing = parameterMapper.selectList(
                Wrappers.<ServiceParameterDefinitionEntity>lambdaQuery()
                        .eq(ServiceParameterDefinitionEntity::getServiceId, definition.serviceId())
        );
        Map<UUID, ServiceParameterDefinitionEntity> existingById = existing.stream()
                .collect(Collectors.toMap(ServiceParameterDefinitionEntity::getId, Function.identity()));
        List<UUID> retainedIds = new ArrayList<>();
        for (ServiceParameterDefinition parameter : definition.parameters()) {
            ServiceParameterDefinitionEntity existingEntity = existingById.get(parameter.parameterId());
            ServiceParameterDefinitionEntity entity = toEntity(parameter, definition, existingEntity);
            retainedIds.add(parameter.parameterId());
            if (existingEntity == null) {
                parameterMapper.insert(entity);
            } else {
                parameterMapper.updateById(entity);
            }
        }
        for (ServiceParameterDefinitionEntity existingEntity : existing) {
            if (!retainedIds.contains(existingEntity.getId())) {
                parameterMapper.deleteById(existingEntity.getId());
            }
        }
    }

    private void syncFieldMappings(DataServiceDefinition definition) {
        List<ServiceFieldMappingEntity> existing = fieldMappingMapper.selectList(
                Wrappers.<ServiceFieldMappingEntity>lambdaQuery()
                        .eq(ServiceFieldMappingEntity::getServiceId, definition.serviceId())
        );
        Map<UUID, ServiceFieldMappingEntity> existingById = existing.stream()
                .collect(Collectors.toMap(ServiceFieldMappingEntity::getId, Function.identity()));
        List<UUID> retainedIds = new ArrayList<>();
        for (ServiceFieldMapping fieldMapping : definition.fieldMappings()) {
            ServiceFieldMappingEntity existingEntity = existingById.get(fieldMapping.mappingId());
            ServiceFieldMappingEntity entity = toEntity(fieldMapping, definition, existingEntity);
            retainedIds.add(fieldMapping.mappingId());
            if (existingEntity == null) {
                fieldMappingMapper.insert(entity);
            } else {
                fieldMappingMapper.updateById(entity);
            }
        }
        for (ServiceFieldMappingEntity existingEntity : existing) {
            if (!retainedIds.contains(existingEntity.getId())) {
                fieldMappingMapper.deleteById(existingEntity.getId());
            }
        }
    }

    private DataServiceDefinition toAggregate(
            DataServiceDefinitionEntity entity,
            List<ServiceParameterDefinitionEntity> parameterEntities,
            List<ServiceFieldMappingEntity> fieldMappingEntities
    ) {
        return new DataServiceDefinition(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getName(),
                DataServiceDefinition.ServiceType.valueOf(entity.getServiceType()),
                DataServiceDefinition.SourceMode.valueOf(entity.getSourceMode()),
                DataServiceDefinition.PermissionMode.valueOf(entity.getPermissionMode()),
                defaultPermissionBoundary(entity.getPermissionBoundaryJson()),
                defaultCachePolicy(entity.getCachePolicyJson()),
                DataServiceDefinition.Status.valueOf(entity.getStatus()),
                entity.getSourceRef(),
                entity.getConnectorId(),
                entity.getDescription(),
                entity.getStatusSequence() == null ? 0 : entity.getStatusSequence(),
                entity.getActivatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                parameterEntities.stream()
                        .map(this::toDomain)
                        .sorted(Comparator.comparingInt(ServiceParameterDefinition::sortOrder)
                                .thenComparing(ServiceParameterDefinition::paramCode))
                        .toList(),
                fieldMappingEntities.stream()
                        .map(this::toDomain)
                        .sorted(Comparator.comparingInt(ServiceFieldMapping::sortOrder)
                                .thenComparing(ServiceFieldMapping::targetField)
                                .thenComparing(ServiceFieldMapping::sourceField))
                        .toList()
        );
    }

    private DataServiceDefinition.PermissionBoundary defaultPermissionBoundary(String json) {
        DataServiceDefinition.PermissionBoundary boundary =
                jsonCodec.read(json, DataServiceDefinition.PermissionBoundary.class);
        return boundary == null ? DataServiceDefinition.PermissionBoundary.none() : boundary;
    }

    private DataServiceDefinition.CachePolicy defaultCachePolicy(String json) {
        DataServiceDefinition.CachePolicy policy = jsonCodec.read(json, DataServiceDefinition.CachePolicy.class);
        return policy == null ? DataServiceDefinition.CachePolicy.disabled() : policy;
    }

    private ServiceParameterDefinition toDomain(ServiceParameterDefinitionEntity entity) {
        return new ServiceParameterDefinition(
                entity.getId(),
                entity.getServiceId(),
                entity.getParamCode(),
                ServiceParameterDefinition.ParameterType.valueOf(entity.getParamType()),
                Boolean.TRUE.equals(entity.getRequired()),
                entity.getDefaultValue(),
                jsonCodec.read(entity.getValidationRuleJson(), ServiceParameterDefinition.ValidationRule.class),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getDescription(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder()
        );
    }

    private ServiceFieldMapping toDomain(ServiceFieldMappingEntity entity) {
        return new ServiceFieldMapping(
                entity.getId(),
                entity.getServiceId(),
                entity.getSourceField(),
                entity.getTargetField(),
                jsonCodec.read(entity.getTransformRuleJson(), ServiceFieldMapping.TransformRule.class),
                Boolean.TRUE.equals(entity.getMasked()),
                entity.getDescription(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder()
        );
    }

    private DataServiceDefinitionEntity toEntity(
            DataServiceDefinition definition,
            DataServiceDefinitionEntity existingEntity
    ) {
        DataServiceDefinitionEntity entity = existingEntity == null
                ? new DataServiceDefinitionEntity()
                : existingEntity;
        entity.setId(definition.serviceId());
        entity.setTenantId(definition.tenantId());
        entity.setCode(definition.code());
        entity.setName(definition.name());
        entity.setServiceType(definition.serviceType().name());
        entity.setSourceMode(definition.sourceMode().name());
        entity.setPermissionMode(definition.permissionMode().name());
        entity.setPermissionBoundaryJson(jsonCodec.write(definition.permissionBoundary()));
        entity.setCachePolicyJson(jsonCodec.write(definition.cachePolicy()));
        entity.setStatus(definition.status().name());
        entity.setSourceRef(definition.sourceRef());
        entity.setConnectorId(definition.connectorId());
        entity.setDescription(definition.description());
        entity.setStatusSequence(definition.statusSequence());
        entity.setActivatedAt(definition.activatedAt());
        entity.setCreatedBy(definition.createdBy());
        entity.setUpdatedBy(definition.updatedBy());
        entity.setCreatedAt(definition.createdAt());
        entity.setUpdatedAt(definition.updatedAt());
        entity.setDeleted(0);
        return entity;
    }

    private ServiceParameterDefinitionEntity toEntity(
            ServiceParameterDefinition parameter,
            DataServiceDefinition definition,
            ServiceParameterDefinitionEntity existingEntity
    ) {
        ServiceParameterDefinitionEntity entity = existingEntity == null
                ? new ServiceParameterDefinitionEntity()
                : existingEntity;
        entity.setId(parameter.parameterId());
        entity.setTenantId(definition.tenantId());
        entity.setServiceId(parameter.serviceId());
        entity.setParamCode(parameter.paramCode());
        entity.setParamType(parameter.paramType().name());
        entity.setRequired(parameter.required());
        entity.setDefaultValue(parameter.defaultValue());
        entity.setValidationRuleJson(jsonCodec.write(parameter.validationRule()));
        entity.setEnabled(parameter.enabled());
        entity.setDescription(parameter.description());
        entity.setSortOrder(parameter.sortOrder());
        entity.setCreatedBy(existingEntity == null ? definition.updatedBy() : existingEntity.getCreatedBy());
        entity.setUpdatedBy(definition.updatedBy());
        entity.setDeleted(0);
        return entity;
    }

    private ServiceFieldMappingEntity toEntity(
            ServiceFieldMapping fieldMapping,
            DataServiceDefinition definition,
            ServiceFieldMappingEntity existingEntity
    ) {
        ServiceFieldMappingEntity entity = existingEntity == null
                ? new ServiceFieldMappingEntity()
                : existingEntity;
        entity.setId(fieldMapping.mappingId());
        entity.setTenantId(definition.tenantId());
        entity.setServiceId(fieldMapping.serviceId());
        entity.setSourceField(fieldMapping.sourceField());
        entity.setTargetField(fieldMapping.targetField());
        entity.setTransformRuleJson(jsonCodec.write(fieldMapping.transformRule()));
        entity.setMasked(fieldMapping.masked());
        entity.setDescription(fieldMapping.description());
        entity.setSortOrder(fieldMapping.sortOrder());
        entity.setCreatedBy(existingEntity == null ? definition.updatedBy() : existingEntity.getCreatedBy());
        entity.setUpdatedBy(definition.updatedBy());
        entity.setDeleted(0);
        return entity;
    }
}
