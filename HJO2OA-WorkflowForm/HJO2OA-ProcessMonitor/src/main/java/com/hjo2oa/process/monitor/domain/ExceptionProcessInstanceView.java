package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record ExceptionProcessInstanceView(
        UUID instanceId,
        UUID definitionId,
        String definitionCode,
        String title,
        String category,
        String status,
        String exceptionType,
        long exceptionMinutes,
        Instant detectedAt
) {
}
