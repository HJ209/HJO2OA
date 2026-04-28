package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.DataReportRefreshedEvent;
import com.hjo2oa.data.report.domain.ReportDataFetchRequest;
import com.hjo2oa.data.report.domain.ReportDataRecord;
import com.hjo2oa.data.report.domain.ReportDataSourceProvider;
import com.hjo2oa.data.report.domain.ReportDataSourceRegistry;
import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotCache;
import com.hjo2oa.data.report.domain.ReportSnapshotPayload;
import com.hjo2oa.data.report.domain.ReportSnapshotRepository;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportRefreshApplicationService {

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportSnapshotRepository reportSnapshotRepository;
    private final ReportSnapshotCache reportSnapshotCache;
    private final ReportDataSourceRegistry reportDataSourceRegistry;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public ReportRefreshApplicationService(
            ReportDefinitionRepository reportDefinitionRepository,
            ReportSnapshotRepository reportSnapshotRepository,
            ReportSnapshotCache reportSnapshotCache,
            ReportDataSourceRegistry reportDataSourceRegistry,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                reportDefinitionRepository,
                reportSnapshotRepository,
                reportSnapshotCache,
                reportDataSourceRegistry,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public ReportRefreshApplicationService(
            ReportDefinitionRepository reportDefinitionRepository,
            ReportSnapshotRepository reportSnapshotRepository,
            ReportSnapshotCache reportSnapshotCache,
            ReportDataSourceRegistry reportDataSourceRegistry,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.reportDefinitionRepository = Objects.requireNonNull(
                reportDefinitionRepository,
                "reportDefinitionRepository must not be null");
        this.reportSnapshotRepository = Objects.requireNonNull(
                reportSnapshotRepository,
                "reportSnapshotRepository must not be null");
        this.reportSnapshotCache = Objects.requireNonNull(reportSnapshotCache, "reportSnapshotCache must not be null");
        this.reportDataSourceRegistry = Objects.requireNonNull(
                reportDataSourceRegistry,
                "reportDataSourceRegistry must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReportSnapshot refreshByCode(
            String code,
            ReportRefreshTriggerMode triggerMode,
            String triggerReason,
            String refreshBatch
    ) {
        ReportDefinition reportDefinition = reportDefinitionRepository.findByCode(code)
                .orElseThrow(() -> new BizException(
                        ReportErrorDescriptors.REPORT_NOT_FOUND,
                        "report definition not found: " + code
                ));
        if (reportDefinition.status() != ReportStatus.ACTIVE && triggerMode != ReportRefreshTriggerMode.MANUAL) {
            throw new BizException(
                    ReportErrorDescriptors.REPORT_RULE_VIOLATION,
                    "report definition is not active: " + code
            );
        }
        String normalizedBatch = refreshBatch == null || refreshBatch.isBlank()
                ? UUID.randomUUID().toString()
                : refreshBatch;
        if (reportSnapshotRepository.existsByReportIdAndRefreshBatch(reportDefinition.id(), normalizedBatch)) {
            return reportSnapshotRepository.findLatestByReportId(reportDefinition.id())
                    .orElseThrow(() -> new BizException(
                            ReportErrorDescriptors.REPORT_SNAPSHOT_NOT_READY,
                            "report snapshot not found for duplicated refresh batch"
                    ));
        }
        ReportDataSourceProvider provider = reportDataSourceRegistry.find(reportDefinition.caliberDefinition().sourceProviderKey())
                .orElseThrow(() -> new BizException(
                        ReportErrorDescriptors.REPORT_DATA_SOURCE_MISSING,
                        "report data source provider not found: " + reportDefinition.caliberDefinition().sourceProviderKey()
                ));
        Instant now = clock.instant();
        try {
            List<ReportDataRecord> rows = provider.fetch(new ReportDataFetchRequest(
                    reportDefinition.code(),
                    reportDefinition.tenantId(),
                    reportDefinition.sourceScope(),
                    reportDefinition.caliberDefinition().baseFilters(),
                    reportDefinition.refreshConfig().effectiveMaxRows(),
                    triggerMode,
                    triggerReason,
                    now
            ));
            ReportSnapshot snapshot = ReportSnapshot.ready(
                    reportDefinition.id(),
                    now,
                    normalizedBatch,
                    new ReportSnapshotPayload(now, provider.providerKey(), rows),
                    triggerMode,
                    triggerReason
            );
            ReportSnapshot persistedSnapshot = reportSnapshotRepository.save(snapshot);
            ReportDefinition updatedDefinition = reportDefinition.markRefreshCompleted(
                    persistedSnapshot.snapshotAt(),
                    ReportFreshnessStatus.READY,
                    persistedSnapshot.refreshBatch()
            );
            reportDefinitionRepository.save(updatedDefinition);
            reportSnapshotCache.put(updatedDefinition.code(), persistedSnapshot);
            domainEventPublisher.publish(DataReportRefreshedEvent.from(updatedDefinition, persistedSnapshot));
            return persistedSnapshot;
        } catch (RuntimeException ex) {
            ReportSnapshot failedSnapshot = ReportSnapshot.failed(
                    reportDefinition.id(),
                    now,
                    normalizedBatch,
                    new ReportSnapshotPayload(now, provider.providerKey(), List.of()),
                    triggerMode,
                    triggerReason,
                    ex.getMessage()
            );
            ReportSnapshot persistedSnapshot = reportSnapshotRepository.save(failedSnapshot);
            ReportDefinition failedDefinition = reportDefinition.markRefreshCompleted(
                    persistedSnapshot.snapshotAt(),
                    ReportFreshnessStatus.FAILED,
                    persistedSnapshot.refreshBatch()
            );
            reportDefinitionRepository.save(failedDefinition);
            throw new BizException(ReportErrorDescriptors.REPORT_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public int refreshDueScheduledReports(String triggerReason) {
        List<ReportDefinition> dueReports = reportDefinitionRepository.findDueScheduledReports(clock.instant());
        int refreshedCount = 0;
        for (ReportDefinition dueReport : dueReports) {
            String batch = "scheduled:" + dueReport.id() + ":" + dueReport.nextRefreshAt();
            try {
                refreshByCode(dueReport.code(), ReportRefreshTriggerMode.SCHEDULED, triggerReason, batch);
                refreshedCount++;
            } catch (RuntimeException ignored) {
                // Preserve failure snapshot and continue refreshing other due reports.
            }
        }
        return refreshedCount;
    }

    public void markStaleIfNecessary(ReportDefinition reportDefinition) {
        if (reportDefinition.lastRefreshedAt() == null) {
            return;
        }
        if (reportDefinition.lastFreshnessStatus() == ReportFreshnessStatus.FAILED) {
            return;
        }
        Instant now = clock.instant();
        if (reportDefinition.lastRefreshedAt()
                .plusSeconds(reportDefinition.refreshConfig().effectiveStaleAfterSeconds())
                .isBefore(now)) {
            reportDefinitionRepository.save(reportDefinition.markStale(now));
            reportSnapshotCache.invalidate(reportDefinition.code());
        }
    }

    public boolean supportsEventDrivenRefresh(ReportDefinition reportDefinition) {
        return reportDefinition.refreshMode() == ReportRefreshMode.EVENT_DRIVEN
                && reportDefinition.status() == ReportStatus.ACTIVE;
    }
}
