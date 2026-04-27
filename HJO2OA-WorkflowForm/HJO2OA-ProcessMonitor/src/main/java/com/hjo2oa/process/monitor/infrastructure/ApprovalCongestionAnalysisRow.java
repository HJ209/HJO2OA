package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalCongestionAnalysisRow {

    private UUID assigneeId;
    private Long pendingCount;
    private Long overdueCount;
    private Instant oldestPendingAt;
    private Instant nearestDueTime;
}
