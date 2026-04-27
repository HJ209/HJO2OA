package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record ApprovalCongestionAnalysisView(
        UUID assigneeId,
        long pendingCount,
        long overdueCount,
        Instant oldestPendingAt,
        Instant nearestDueTime
) {
}
