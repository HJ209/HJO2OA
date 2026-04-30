package com.hjo2oa.wf.process.definition.domain.model;

import java.util.List;
import java.util.Map;

public record WorkflowNodeDefinition(
        String nodeId,
        String name,
        String type,
        WorkflowParticipantRule participantRule,
        List<String> actionCodes,
        String multiInstanceType,
        String completionCondition,
        Map<String, Object> attributes
) {

    public boolean isStart() {
        return "START".equalsIgnoreCase(type);
    }

    public boolean isEnd() {
        return "END".equalsIgnoreCase(type);
    }

    public boolean isUserTask() {
        return "USER_TASK".equalsIgnoreCase(type) || "USER_TASK_NODE".equalsIgnoreCase(type);
    }
}
