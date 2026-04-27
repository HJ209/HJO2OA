package com.hjo2oa.process.monitor.domain;

import java.util.UUID;

public record ProcessDurationAnalysisView(
        UUID definitionId,
        String definitionCode,
        String category,
        long instanceCount,
        long completedCount,
        long runningCount,
        long averageDurationMinutes,
        long maxDurationMinutes
) {
}
