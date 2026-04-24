package com.hjo2oa.data.report.interfaces;

import com.hjo2oa.data.report.domain.ReportSnapshot;
import java.time.Instant;

public record ReportSnapshotView(
        String id,
        Instant snapshotAt,
        String refreshBatch,
        String freshnessStatus,
        String triggerMode,
        String triggerReason,
        String errorMessage,
        int rowCount
) {

    public static ReportSnapshotView from(ReportSnapshot snapshot) {
        return new ReportSnapshotView(
                snapshot.id(),
                snapshot.snapshotAt(),
                snapshot.refreshBatch(),
                snapshot.freshnessStatus().name(),
                snapshot.triggerMode().name(),
                snapshot.triggerReason(),
                snapshot.errorMessage(),
                snapshot.payload().rows().size()
        );
    }
}
