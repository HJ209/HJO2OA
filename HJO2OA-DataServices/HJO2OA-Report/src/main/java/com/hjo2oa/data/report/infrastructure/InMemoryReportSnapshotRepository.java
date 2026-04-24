package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotPage;
import com.hjo2oa.data.report.domain.ReportSnapshotRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryReportSnapshotRepository implements ReportSnapshotRepository {

    private final List<ReportSnapshot> snapshots = new ArrayList<>();

    @Override
    public ReportSnapshot save(ReportSnapshot snapshot) {
        ReportSnapshot persisted = new ReportSnapshot(
                snapshot.id() == null ? UUID.randomUUID().toString() : snapshot.id(),
                snapshot.reportId(),
                snapshot.snapshotAt(),
                snapshot.refreshBatch(),
                snapshot.scopeSignature(),
                snapshot.payload(),
                snapshot.freshnessStatus(),
                snapshot.triggerMode(),
                snapshot.triggerReason(),
                snapshot.errorMessage()
        );
        snapshots.add(persisted);
        return persisted;
    }

    @Override
    public Optional<ReportSnapshot> findLatestByReportId(String reportId) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.reportId().equals(reportId))
                .max(Comparator.comparing(ReportSnapshot::snapshotAt));
    }

    @Override
    public Optional<ReportSnapshot> findLatestReadyByReportId(String reportId) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.reportId().equals(reportId))
                .filter(snapshot -> snapshot.freshnessStatus() == ReportFreshnessStatus.READY)
                .max(Comparator.comparing(ReportSnapshot::snapshotAt));
    }

    @Override
    public boolean existsByReportIdAndRefreshBatch(String reportId, String refreshBatch) {
        return snapshots.stream()
                .anyMatch(snapshot -> snapshot.reportId().equals(reportId)
                        && snapshot.refreshBatch().equals(refreshBatch));
    }

    @Override
    public ReportSnapshotPage pageByReportId(String reportId, int page, int size) {
        List<ReportSnapshot> items = snapshots.stream()
                .filter(snapshot -> snapshot.reportId().equals(reportId))
                .sorted(Comparator.comparing(ReportSnapshot::snapshotAt).reversed())
                .toList();
        int fromIndex = Math.min((page - 1) * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return new ReportSnapshotPage(items.subList(fromIndex, toIndex), items.size());
    }
}
