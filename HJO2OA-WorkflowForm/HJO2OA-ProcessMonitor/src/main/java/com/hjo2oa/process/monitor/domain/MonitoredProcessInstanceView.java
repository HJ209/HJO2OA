package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record MonitoredProcessInstanceView(
        UUID instanceId,
        UUID definitionId,
        String definitionCode,
        String title,
        String category,
        UUID initiatorId,
        String status,
        Instant startTime,
        Instant endTime,
        Instant updatedAt
) {
}
