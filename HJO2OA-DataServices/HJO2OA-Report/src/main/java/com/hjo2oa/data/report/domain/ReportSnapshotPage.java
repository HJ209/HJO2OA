package com.hjo2oa.data.report.domain;

import java.util.List;

public record ReportSnapshotPage(
        List<ReportSnapshot> items,
        long total
) {

    public ReportSnapshotPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
