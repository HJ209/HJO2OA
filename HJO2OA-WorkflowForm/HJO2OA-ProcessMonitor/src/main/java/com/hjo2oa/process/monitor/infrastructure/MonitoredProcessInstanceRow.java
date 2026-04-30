package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitoredProcessInstanceRow {
    private UUID instanceId;
    private UUID definitionId;
    private String definitionCode;
    private String title;
    private String category;
    private UUID initiatorId;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private Instant updatedAt;
}
