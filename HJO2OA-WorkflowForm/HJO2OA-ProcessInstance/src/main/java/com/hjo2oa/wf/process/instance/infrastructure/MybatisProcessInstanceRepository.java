package com.hjo2oa.wf.process.instance.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisProcessInstanceRepository implements ProcessInstanceRepository {

    private final ProcessInstanceMapper mapper;
    private final ProcessInstanceJsonCodec jsonCodec;

    public MybatisProcessInstanceRepository(ProcessInstanceMapper mapper, ProcessInstanceJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Optional<ProcessInstance> findById(UUID instanceId) {
        return Optional.ofNullable(mapper.selectById(instanceId)).map(this::toDomain);
    }

    @Override
    public Optional<ProcessInstance> findByTenantAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ProcessInstanceEntity>lambdaQuery()
                        .eq(ProcessInstanceEntity::getTenantId, tenantId)
                        .eq(ProcessInstanceEntity::getIdempotencyKey, idempotencyKey)))
                .map(this::toDomain);
    }

    @Override
    public List<ProcessInstance> findByInitiator(UUID tenantId, UUID initiatorId) {
        return mapper.selectList(Wrappers.<ProcessInstanceEntity>lambdaQuery()
                        .eq(ProcessInstanceEntity::getTenantId, tenantId)
                        .eq(ProcessInstanceEntity::getInitiatorId, initiatorId)
                        .orderByDesc(ProcessInstanceEntity::getStartTime))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ProcessInstance save(ProcessInstance instance) {
        ProcessInstanceEntity entity = toEntity(instance);
        if (mapper.selectById(instance.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(instance.id()).orElseThrow();
    }

    private ProcessInstance toDomain(ProcessInstanceEntity entity) {
        return new ProcessInstance(
                entity.getId(),
                entity.getDefinitionId(),
                entity.getDefinitionVersion(),
                entity.getDefinitionCode(),
                entity.getBusinessKey(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getInitiatorId(),
                entity.getInitiatorOrgId(),
                entity.getInitiatorDeptId(),
                entity.getInitiatorPositionId(),
                entity.getFormMetadataId(),
                entity.getFormDataId(),
                jsonCodec.readStringList(entity.getCurrentNodes()),
                ProcessInstanceStatus.valueOf(entity.getStatus()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTenantId(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProcessInstanceEntity toEntity(ProcessInstance instance) {
        return new ProcessInstanceEntity()
                .setId(instance.id())
                .setDefinitionId(instance.definitionId())
                .setDefinitionVersion(instance.definitionVersion())
                .setDefinitionCode(instance.definitionCode())
                .setBusinessKey(instance.businessKey())
                .setTitle(instance.title())
                .setCategory(instance.category())
                .setInitiatorId(instance.initiatorId())
                .setInitiatorOrgId(instance.initiatorOrgId())
                .setInitiatorDeptId(instance.initiatorDeptId())
                .setInitiatorPositionId(instance.initiatorPositionId())
                .setFormMetadataId(instance.formMetadataId())
                .setFormDataId(instance.formDataId())
                .setCurrentNodes(jsonCodec.write(instance.currentNodes()))
                .setStatus(instance.status().name())
                .setStartTime(instance.startTime())
                .setEndTime(instance.endTime())
                .setTenantId(instance.tenantId())
                .setIdempotencyKey(instance.idempotencyKey())
                .setCreatedAt(instance.createdAt())
                .setUpdatedAt(instance.updatedAt());
    }
}
