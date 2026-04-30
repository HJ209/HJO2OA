package com.hjo2oa.wf.process.instance.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.wf.process.instance.domain.ProcessHistoryRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisProcessHistoryRepository implements ProcessHistoryRepository {

    private final NodeHistoryMapper nodeHistoryMapper;
    private final VariableHistoryMapper variableHistoryMapper;
    private final ProcessInstanceJsonCodec jsonCodec;

    public MybatisProcessHistoryRepository(
            NodeHistoryMapper nodeHistoryMapper,
            VariableHistoryMapper variableHistoryMapper,
            ProcessInstanceJsonCodec jsonCodec
    ) {
        this.nodeHistoryMapper = nodeHistoryMapper;
        this.variableHistoryMapper = variableHistoryMapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void recordNode(
            TaskInstance task,
            String historyStatus,
            String actionCode,
            UUID operatorId,
            Instant occurredAt
    ) {
        nodeHistoryMapper.insert(new NodeHistoryEntity()
                .setId(UUID.randomUUID())
                .setInstanceId(task.instanceId())
                .setTaskId(task.id())
                .setNodeId(task.nodeId())
                .setNodeName(task.nodeName())
                .setNodeType(task.nodeType().name())
                .setHistoryStatus(historyStatus)
                .setActionCode(actionCode)
                .setOperatorId(operatorId)
                .setOccurredAt(occurredAt)
                .setTenantId(task.tenantId()));
    }

    @Override
    public void recordVariables(
            UUID instanceId,
            UUID taskId,
            Map<String, Object> variables,
            UUID operatorId,
            UUID tenantId,
            Instant occurredAt
    ) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        variables.forEach((name, value) -> variableHistoryMapper.insert(new VariableHistoryEntity()
                .setId(UUID.randomUUID())
                .setInstanceId(instanceId)
                .setTaskId(taskId)
                .setVariableName(name)
                .setOldValue(null)
                .setNewValue(jsonCodec.write(value))
                .setOperatorId(operatorId)
                .setOccurredAt(occurredAt)
                .setTenantId(tenantId)));
    }

    @Override
    public List<ProcessInstanceViews.NodeHistoryView> findNodeHistory(UUID instanceId) {
        return nodeHistoryMapper.selectList(Wrappers.<NodeHistoryEntity>lambdaQuery()
                        .eq(NodeHistoryEntity::getInstanceId, instanceId)
                        .orderByAsc(NodeHistoryEntity::getOccurredAt))
                .stream()
                .map(this::toNodeView)
                .toList();
    }

    @Override
    public List<ProcessInstanceViews.VariableHistoryView> findVariableHistory(UUID instanceId) {
        return variableHistoryMapper.selectList(Wrappers.<VariableHistoryEntity>lambdaQuery()
                        .eq(VariableHistoryEntity::getInstanceId, instanceId)
                        .orderByAsc(VariableHistoryEntity::getOccurredAt))
                .stream()
                .map(this::toVariableView)
                .toList();
    }

    private ProcessInstanceViews.NodeHistoryView toNodeView(NodeHistoryEntity entity) {
        return new ProcessInstanceViews.NodeHistoryView(
                entity.getId(),
                entity.getInstanceId(),
                entity.getTaskId(),
                entity.getNodeId(),
                entity.getNodeName(),
                TaskNodeType.valueOf(entity.getNodeType()),
                entity.getHistoryStatus(),
                entity.getActionCode(),
                entity.getOperatorId(),
                entity.getOccurredAt(),
                entity.getTenantId()
        );
    }

    private ProcessInstanceViews.VariableHistoryView toVariableView(VariableHistoryEntity entity) {
        return new ProcessInstanceViews.VariableHistoryView(
                entity.getId(),
                entity.getInstanceId(),
                entity.getTaskId(),
                entity.getVariableName(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getOperatorId(),
                entity.getOccurredAt(),
                entity.getTenantId()
        );
    }
}
