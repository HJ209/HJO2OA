package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeTrailRow {
    private UUID taskId;
    private UUID instanceId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private UUID assigneeId;
    private String taskStatus;
    private Instant createdAt;
    private Instant claimTime;
    private Instant completedTime;
    private Instant dueTime;
    private String lastActionCode;
    private String lastActionName;
    private UUID lastOperatorId;
    private Instant lastActionAt;
}
