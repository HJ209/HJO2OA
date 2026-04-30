package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessInterventionRow {
    private UUID interventionId;
    private UUID instanceId;
    private UUID taskId;
    private String actionType;
    private UUID operatorId;
    private UUID targetAssigneeId;
    private String reason;
    private Instant createdAt;
}
