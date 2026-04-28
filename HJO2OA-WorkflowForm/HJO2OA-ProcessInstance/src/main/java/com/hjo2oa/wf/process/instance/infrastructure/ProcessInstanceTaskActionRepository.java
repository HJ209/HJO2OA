package com.hjo2oa.wf.process.instance.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.instance.domain.TaskAction;
import com.hjo2oa.wf.process.instance.domain.TaskActionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ProcessInstanceTaskActionRepository implements TaskActionRepository {

    private final TaskActionMapper mapper;
    private final ProcessInstanceJsonCodec jsonCodec;

    public ProcessInstanceTaskActionRepository(TaskActionMapper mapper, ProcessInstanceJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public List<TaskAction> findByInstanceId(UUID instanceId) {
        return mapper.selectList(Wrappers.<TaskActionEntity>lambdaQuery()
                        .eq(TaskActionEntity::getInstanceId, instanceId)
                        .orderByAsc(TaskActionEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TaskAction> findByTaskId(UUID taskId) {
        return mapper.selectList(Wrappers.<TaskActionEntity>lambdaQuery()
                        .eq(TaskActionEntity::getTaskId, taskId)
                        .orderByAsc(TaskActionEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TaskAction save(TaskAction action) {
        mapper.insert(toEntity(action));
        return action;
    }

    private TaskAction toDomain(TaskActionEntity entity) {
        return new TaskAction(
                entity.getId(),
                entity.getTaskId(),
                entity.getInstanceId(),
                entity.getActionCode(),
                entity.getActionName(),
                entity.getOperatorId(),
                entity.getOperatorOrgId(),
                entity.getOperatorPositionId(),
                entity.getOpinion(),
                entity.getTargetNodeId(),
                jsonCodec.readMap(entity.getFormDataPatch()),
                entity.getCreatedAt()
        );
    }

    private TaskActionEntity toEntity(TaskAction action) {
        return new TaskActionEntity()
                .setId(action.id())
                .setTaskId(action.taskId())
                .setInstanceId(action.instanceId())
                .setActionCode(action.actionCode())
                .setActionName(action.actionName())
                .setOperatorId(action.operatorId())
                .setOperatorOrgId(action.operatorOrgId())
                .setOperatorPositionId(action.operatorPositionId())
                .setOpinion(action.opinion())
                .setTargetNodeId(action.targetNodeId())
                .setFormDataPatch(jsonCodec.write(action.formDataPatch()))
                .setCreatedAt(action.createdAt());
    }
}
