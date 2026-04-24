package com.hjo2oa.data.report.domain;

import java.util.Optional;

public interface ReportSnapshotCache {

    Optional<ReportSnapshot> findReadySnapshotByCode(String reportCode);

    void put(String reportCode, ReportSnapshot snapshot);

    void invalidate(String reportCode);
}
