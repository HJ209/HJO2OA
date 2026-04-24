package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;

public record ReportSnapshotPayload(
        Instant generatedAt,
        String sourceProviderKey,
        List<ReportDataRecord> rows
) {

    public ReportSnapshotPayload {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
