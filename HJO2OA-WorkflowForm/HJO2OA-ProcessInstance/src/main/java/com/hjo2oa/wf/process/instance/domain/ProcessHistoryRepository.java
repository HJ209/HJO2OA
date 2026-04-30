package com.hjo2oa.wf.process.instance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProcessHistoryRepository {

    void recordNode(
            TaskInstance task,
            String historyStatus,
            String actionCode,
            UUID operatorId,
            Instant occurredAt
    );

    void recordVariables(
            UUID instanceId,
            UUID taskId,
            Map<String, Object> variables,
            UUID operatorId,
            UUID tenantId,
            Instant occurredAt
    );

    List<ProcessInstanceViews.NodeHistoryView> findNodeHistory(UUID instanceId);

    List<ProcessInstanceViews.VariableHistoryView> findVariableHistory(UUID instanceId);
}
