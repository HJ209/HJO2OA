package com.hjo2oa.data.report.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record ReportSnapshotPayload(
        Instant generatedAt,
        String sourceProviderKey,
        List<ReportDataRecord> rows
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ReportSnapshotPayload {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
