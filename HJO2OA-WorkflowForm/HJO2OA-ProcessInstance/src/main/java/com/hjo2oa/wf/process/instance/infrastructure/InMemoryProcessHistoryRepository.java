package com.hjo2oa.wf.process.instance.infrastructure;

import com.hjo2oa.wf.process.instance.domain.ProcessHistoryRepository;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryProcessHistoryRepository implements ProcessHistoryRepository {

    private final List<ProcessInstanceViews.NodeHistoryView> nodeHistory = new CopyOnWriteArrayList<>();
    private final List<ProcessInstanceViews.VariableHistoryView> variableHistory = new CopyOnWriteArrayList<>();

    @Override
    public void recordNode(
            TaskInstance task,
            String historyStatus,
            String actionCode,
            UUID operatorId,
            Instant occurredAt
    ) {
        nodeHistory.add(new ProcessInstanceViews.NodeHistoryView(
                UUID.randomUUID(),
                task.instanceId(),
                task.id(),
                task.nodeId(),
                task.nodeName(),
                task.nodeType(),
                historyStatus,
                actionCode,
                operatorId,
                occurredAt,
                task.tenantId()
        ));
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
        variables.forEach((name, value) -> variableHistory.add(new ProcessInstanceViews.VariableHistoryView(
                UUID.randomUUID(),
                instanceId,
                taskId,
                name,
                null,
                value == null ? null : String.valueOf(value),
                operatorId,
                occurredAt,
                tenantId
        )));
    }

    @Override
    public List<ProcessInstanceViews.NodeHistoryView> findNodeHistory(UUID instanceId) {
        return nodeHistory.stream()
                .filter(history -> instanceId.equals(history.instanceId()))
                .sorted(Comparator.comparing(ProcessInstanceViews.NodeHistoryView::occurredAt))
                .toList();
    }

    @Override
    public List<ProcessInstanceViews.VariableHistoryView> findVariableHistory(UUID instanceId) {
        return variableHistory.stream()
                .filter(history -> instanceId.equals(history.instanceId()))
                .sorted(Comparator.comparing(ProcessInstanceViews.VariableHistoryView::occurredAt))
                .toList();
    }
}
