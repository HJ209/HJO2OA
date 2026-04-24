package com.hjo2oa.data.report.domain;

import java.util.Optional;

public interface ReportSnapshotRepository {

    ReportSnapshot save(ReportSnapshot snapshot);

    Optional<ReportSnapshot> findLatestByReportId(String reportId);

    Optional<ReportSnapshot> findLatestReadyByReportId(String reportId);

    boolean existsByReportIdAndRefreshBatch(String reportId, String refreshBatch);

    ReportSnapshotPage pageByReportId(String reportId, int page, int size);
}
