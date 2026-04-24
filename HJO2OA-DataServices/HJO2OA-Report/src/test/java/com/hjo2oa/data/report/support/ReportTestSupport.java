package com.hjo2oa.data.report.support;

import com.hjo2oa.data.report.application.SaveReportDefinitionCommand;
import com.hjo2oa.data.report.domain.ReportCaliberDefinition;
import com.hjo2oa.data.report.domain.ReportCardProtocol;
import com.hjo2oa.data.report.domain.ReportCardType;
import com.hjo2oa.data.report.domain.ReportDataFetchRequest;
import com.hjo2oa.data.report.domain.ReportDataRecord;
import com.hjo2oa.data.report.domain.ReportDataSourceProvider;
import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionPage;
import com.hjo2oa.data.report.domain.ReportDefinitionQuery;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportDimensionType;
import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportMetricAggregationType;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportRefreshConfig;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotPage;
import com.hjo2oa.data.report.domain.ReportSnapshotRepository;
import com.hjo2oa.data.report.domain.ReportSourceScope;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.data.report.domain.ReportTimeGranularity;
import com.hjo2oa.data.report.domain.ReportType;
import com.hjo2oa.data.report.domain.ReportVisibilityMode;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ReportTestSupport {

    public static final Instant FIXED_TIME = Instant.parse("2026-04-24T01:00:00Z");
    public static final String TENANT_ID = "tenant-1";
    public static final String PROVIDER_KEY = "task-pressure-provider";

    private ReportTestSupport() {
    }

    public static Clock fixedClock() {
        return Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
    }

    public static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    public static SaveReportDefinitionCommand sampleCommand(String code, ReportRefreshMode refreshMode) {
        return new SaveReportDefinitionCommand(
                code,
                "任务压力统计",
                ReportType.CARD,
                ReportSourceScope.TASK,
                refreshMode,
                ReportVisibilityMode.PORTAL_CARD,
                TENANT_ID,
                new ReportCaliberDefinition(
                        PROVIDER_KEY,
                        "TASK",
                        "occurredAt",
                        "organizationCode",
                        "taskDataService",
                        Map.of("scene", "office-center"),
                        refreshMode == ReportRefreshMode.EVENT_DRIVEN ? List.of("process.*") : List.of(),
                        "任务压力平台统计口径"
                ),
                new ReportRefreshConfig(300, 900, 1000),
                new ReportCardProtocol(
                        "task-pressure",
                        "待办压力",
                        ReportCardType.MIXED,
                        "volume",
                        "volume",
                        "volume",
                        "organization",
                        5
                ),
                List.of(
                        new ReportMetricDefinition(
                                null,
                                "volume",
                                "任务总量",
                                ReportMetricAggregationType.SUM,
                                "totalCount",
                                null,
                                null,
                                "件",
                                true,
                                true,
                                0
                        ),
                        new ReportMetricDefinition(
                                null,
                                "completionRate",
                                "完成率",
                                ReportMetricAggregationType.RATIO,
                                null,
                                "sum(completedCount)/sum(totalCount)",
                                null,
                                "%",
                                true,
                                false,
                                1
                        )
                ),
                List.of(
                        new ReportDimensionDefinition(
                                null,
                                "day",
                                "统计日",
                                ReportDimensionType.TIME,
                                "occurredAt",
                                ReportTimeGranularity.DAY,
                                true,
                                0
                        ),
                        new ReportDimensionDefinition(
                                null,
                                "organization",
                                "组织",
                                ReportDimensionType.ORGANIZATION,
                                "organizationCode",
                                ReportTimeGranularity.NONE,
                                true,
                                1
                        )
                )
        );
    }

    public static ReportDataSourceProvider sampleProvider() {
        return new ReportDataSourceProvider() {
            @Override
            public String providerKey() {
                return PROVIDER_KEY;
            }

            @Override
            public List<ReportDataRecord> fetch(ReportDataFetchRequest request) {
                return List.of(
                        new ReportDataRecord(
                                Instant.parse("2026-04-23T00:00:00Z"),
                                Map.of(
                                        "occurredAt", "2026-04-23T00:00:00Z",
                                        "organizationCode", "org-a",
                                        "totalCount", 10,
                                        "completedCount", 7,
                                        "scene", "office-center"
                                )
                        ),
                        new ReportDataRecord(
                                Instant.parse("2026-04-24T00:00:00Z"),
                                Map.of(
                                        "occurredAt", "2026-04-24T00:00:00Z",
                                        "organizationCode", "org-a",
                                        "totalCount", 20,
                                        "completedCount", 16,
                                        "scene", "office-center"
                                )
                        ),
                        new ReportDataRecord(
                                Instant.parse("2026-04-24T00:00:00Z"),
                                Map.of(
                                        "occurredAt", "2026-04-24T00:00:00Z",
                                        "organizationCode", "org-b",
                                        "totalCount", 5,
                                        "completedCount", 3,
                                        "scene", "office-center"
                                )
                        )
                );
            }
        };
    }

    public static DomainEvent simpleEvent(String eventType) {
        return new DomainEvent() {
            @Override
            public UUID eventId() {
                return UUID.randomUUID();
            }

            @Override
            public String eventType() {
                return eventType;
            }

            @Override
            public Instant occurredAt() {
                return FIXED_TIME;
            }

            @Override
            public String tenantId() {
                return TENANT_ID;
            }
        };
    }

    public static final class InMemoryReportDefinitionRepository implements ReportDefinitionRepository {

        private final Map<String, ReportDefinition> byCode = new LinkedHashMap<>();
        private final Map<String, ReportDefinition> byId = new LinkedHashMap<>();

        @Override
        public Optional<ReportDefinition> findByCode(String code) {
            return Optional.ofNullable(byCode.get(code));
        }

        @Override
        public Optional<ReportDefinition> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public ReportDefinition save(ReportDefinition reportDefinition) {
            String id = reportDefinition.id() == null ? UUID.randomUUID().toString() : reportDefinition.id();
            ReportDefinition persisted = new ReportDefinition(
                    id,
                    reportDefinition.code(),
                    reportDefinition.name(),
                    reportDefinition.reportType(),
                    reportDefinition.sourceScope(),
                    reportDefinition.refreshMode(),
                    reportDefinition.visibilityMode(),
                    reportDefinition.status(),
                    reportDefinition.tenantId(),
                    reportDefinition.definitionVersion(),
                    reportDefinition.caliberDefinition(),
                    reportDefinition.refreshConfig(),
                    reportDefinition.cardProtocol(),
                    reportDefinition.lastRefreshedAt(),
                    reportDefinition.lastFreshnessStatus(),
                    reportDefinition.lastRefreshBatch(),
                    reportDefinition.nextRefreshAt(),
                    reportDefinition.createdAt(),
                    reportDefinition.updatedAt(),
                    reportDefinition.metrics(),
                    reportDefinition.dimensions()
            );
            byCode.put(persisted.code(), persisted);
            byId.put(persisted.id(), persisted);
            return persisted;
        }

        @Override
        public ReportDefinitionPage page(ReportDefinitionQuery query) {
            List<ReportDefinition> allItems = byCode.values().stream()
                    .filter(item -> query.reportType() == null || item.reportType() == query.reportType())
                    .filter(item -> query.sourceScope() == null || item.sourceScope() == query.sourceScope())
                    .filter(item -> query.status() == null || item.status() == query.status())
                    .filter(item -> query.visibilityMode() == null || item.visibilityMode() == query.visibilityMode())
                    .sorted(Comparator.comparing(ReportDefinition::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            int fromIndex = Math.min((query.page() - 1) * query.size(), allItems.size());
            int toIndex = Math.min(fromIndex + query.size(), allItems.size());
            return new ReportDefinitionPage(allItems.subList(fromIndex, toIndex), allItems.size());
        }

        @Override
        public List<ReportDefinition> findByRefreshModeAndStatus(ReportRefreshMode refreshMode, ReportStatus status) {
            return byCode.values().stream()
                    .filter(item -> item.refreshMode() == refreshMode && item.status() == status)
                    .toList();
        }

        @Override
        public List<ReportDefinition> findDueScheduledReports(Instant now) {
            return byCode.values().stream()
                    .filter(item -> item.dueForScheduledRefresh(now))
                    .toList();
        }
    }

    public static final class InMemoryReportSnapshotRepository implements ReportSnapshotRepository {

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

    public static final class RecordingDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        public List<DomainEvent> events() {
            return List.copyOf(events);
        }
    }
}
