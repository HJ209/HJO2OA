package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotCache;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hjo2oa.cache.type", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryReportSnapshotCache implements ReportSnapshotCache {

    private final Map<String, ReportSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<ReportSnapshot> findReadySnapshotByCode(String reportCode) {
        return Optional.ofNullable(snapshots.get(reportCode));
    }

    @Override
    public void put(String reportCode, ReportSnapshot snapshot) {
        snapshots.put(reportCode, snapshot);
    }

    @Override
    public void invalidate(String reportCode) {
        snapshots.remove(reportCode);
    }
}
