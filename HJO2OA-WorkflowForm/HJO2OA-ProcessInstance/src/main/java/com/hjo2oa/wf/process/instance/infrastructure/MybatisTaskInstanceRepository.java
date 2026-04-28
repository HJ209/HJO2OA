package com.hjo2oa.wf.process.instance.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTaskInstanceRepository implements TaskInstanceRepository {

    private final TaskInstanceMapper mapper;
    private final ProcessInstanceJsonCodec jsonCodec;

    public MybatisTaskInstanceRepository(TaskInstanceMapper mapper, ProcessInstanceJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public java.util.Optional<TaskInstance> findById(UUID taskId) {
        return java.util.Optional.ofNullable(mapper.selectById(taskId)).map(this::toDomain);
    }

    @Override
    public List<TaskInstance> findByInstanceId(UUID instanceId) {
        return mapper.selectList(Wrappers.<TaskInstanceEntity>lambdaQuery()
                        .eq(TaskInstanceEntity::getInstanceId, instanceId)
                        .orderByAsc(TaskInstanceEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TaskInstance> findOpenByInstanceId(UUID instanceId) {
        return mapper.selectList(Wrappers.<TaskInstanceEntity>lambdaQuery()
                        .eq(TaskInstanceEntity::getInstanceId, instanceId)
                        .in(TaskInstanceEntity::getStatus, TaskInstanceStatus.CREATED.name(), TaskInstanceStatus.CLAIMED.name()))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TaskInstance> findOpenByNode(UUID instanceId, String nodeId) {
        return mapper.selectList(Wrappers.<TaskInstanceEntity>lambdaQuery()
                        .eq(TaskInstanceEntity::getInstanceId, instanceId)
                        .eq(TaskInstanceEntity::getNodeId, nodeId)
                        .in(TaskInstanceEntity::getStatus, TaskInstanceStatus.CREATED.name(), TaskInstanceStatus.CLAIMED.name()))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TaskInstance save(TaskInstance task) {
        TaskInstanceEntity entity = toEntity(task);
        if (mapper.selectById(task.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findById(task.id()).orElseThrow();
    }

    @Override
    public List<TaskInstance> saveAll(List<TaskInstance> tasks) {
        return tasks.stream().map(this::save).toList();
    }

    private TaskInstance toDomain(TaskInstanceEntity entity) {
        return new TaskInstance(
                entity.getId(),
                entity.getInstanceId(),
                entity.getNodeId(),
                entity.getNodeName(),
                TaskNodeType.valueOf(entity.getNodeType()),
                entity.getAssigneeId(),
                entity.getAssigneeOrgId(),
                entity.getAssigneeDeptId(),
                entity.getAssigneePositionId(),
                valueOfNullable(CandidateType.class, entity.getCandidateType()),
                jsonCodec.readUuidList(entity.getCandidateIds()),
                valueOfNullable(MultiInstanceType.class, entity.getMultiInstanceType()),
                entity.getCompletionCondition(),
                TaskInstanceStatus.valueOf(entity.getStatus()),
                entity.getClaimTime(),
                entity.getCompletedTime(),
                entity.getDueTime(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TaskInstanceEntity toEntity(TaskInstance task) {
        return new TaskInstanceEntity()
                .setId(task.id())
                .setInstanceId(task.instanceId())
                .setNodeId(task.nodeId())
                .setNodeName(task.nodeName())
                .setNodeType(task.nodeType().name())
                .setAssigneeId(task.assigneeId())
                .setAssigneeOrgId(task.assigneeOrgId())
                .setAssigneeDeptId(task.assigneeDeptId())
                .setAssigneePositionId(task.assigneePositionId())
                .setCandidateType(task.candidateType() == null ? null : task.candidateType().name())
                .setCandidateIds(jsonCodec.write(task.candidateIds()))
                .setMultiInstanceType(task.multiInstanceType().name())
                .setCompletionCondition(task.completionCondition())
                .setStatus(task.status().name())
                .setClaimTime(task.claimTime())
                .setCompletedTime(task.completedTime())
                .setDueTime(task.dueTime())
                .setTenantId(task.tenantId())
                .setCreatedAt(task.createdAt())
                .setUpdatedAt(task.updatedAt());
    }

    private static <E extends Enum<E>> E valueOfNullable(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumType, value);
    }
}
