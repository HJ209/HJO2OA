package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeStagnationAnalysisRow {

    private UUID taskId;
    private UUID instanceId;
    private String instanceTitle;
    private UUID definitionId;
    private String definitionCode;
    private String category;
    private String nodeId;
    private String nodeName;
    private UUID assigneeId;
    private String taskStatus;
    private Instant taskCreatedAt;
    private Instant dueTime;
    private Long stalledMinutes;
}
