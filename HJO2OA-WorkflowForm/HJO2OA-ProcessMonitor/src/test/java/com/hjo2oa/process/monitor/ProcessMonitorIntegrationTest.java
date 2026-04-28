package com.hjo2oa.process.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.process.monitor.application.ProcessMonitorQueryApplicationService;
import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessMonitorQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessMonitorIntegrationTest {

    private final ReadOnlyRepository repository = new ReadOnlyRepository();
    private final ProcessMonitorQueryApplicationService service =
            new ProcessMonitorQueryApplicationService(repository);

    @Test
    void shouldQueryAllMonitorReadModelsWithoutWritesOrEvents() {
        MonitorQueryFilter filter = MonitorQueryFilter.of(
                UUID.randomUUID(),
                null,
                "expense",
                "finance",
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-27T00:00:00Z"),
                20,
                60L
        );

        List<OverdueTaskObservationView> overdueTasks = service.overdueTasks(filter);
        assertThat(overdueTasks).singleElement().satisfies(view -> {
            assertThat(view.taskId()).isNotNull();
            assertThat(view.instanceId()).isNotNull();
            assertThat(view.overdueMinutes()).isEqualTo(90);
        });

        List<NodeStagnationAnalysisView> stalledNodes = service.stalledNodes(filter);
        assertThat(stalledNodes).singleElement().satisfies(view -> {
            assertThat(view.nodeId()).isEqualTo("managerApprove");
            assertThat(view.stalledMinutes()).isEqualTo(180);
        });

        List<ApprovalCongestionAnalysisView> congestion = service.approvalCongestion(filter);
        assertThat(congestion).singleElement().satisfies(view -> {
            assertThat(view.assigneeId()).isNotNull();
            assertThat(view.pendingCount()).isEqualTo(6);
        });

        List<ProcessDurationAnalysisView> durations = service.processDurations(filter);
        assertThat(durations).singleElement().satisfies(view -> {
            assertThat(view.definitionId()).isNotNull();
            assertThat(view.averageDurationMinutes()).isEqualTo(42);
        });

        assertThat(repository.writeCount).isZero();
        assertThat(repository.publishedEventCount).isZero();
        assertThat(repository.queryCount).isEqualTo(4);
    }

    private static final class ReadOnlyRepository implements ProcessMonitorQueryRepository {
        private int queryCount;
        private int writeCount;
        private int publishedEventCount;

        @Override
        public List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter) {
            queryCount++;
            return List.of(new ProcessDurationAnalysisView(
                    UUID.randomUUID(),
                    "expense",
                    "finance",
                    3,
                    2,
                    1,
                    42,
                    120
            ));
        }

        @Override
        public List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter) {
            queryCount++;
            return List.of(new NodeStagnationAnalysisView(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Expense approval",
                    UUID.randomUUID(),
                    "expense",
                    "finance",
                    "managerApprove",
                    "Manager approve",
                    UUID.randomUUID(),
                    "CLAIMED",
                    Instant.parse("2026-04-26T20:00:00Z"),
                    null,
                    180
            ));
        }

        @Override
        public List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter) {
            queryCount++;
            return List.of(new ApprovalCongestionAnalysisView(
                    UUID.randomUUID(),
                    6,
                    2,
                    Instant.parse("2026-04-26T08:00:00Z"),
                    Instant.parse("2026-04-27T08:00:00Z")
            ));
        }

        @Override
        public List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter) {
            queryCount++;
            return List.of(new OverdueTaskObservationView(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Expense approval",
                    UUID.randomUUID(),
                    "expense",
                    "finance",
                    "managerApprove",
                    "Manager approve",
                    UUID.randomUUID(),
                    Instant.parse("2026-04-27T08:00:00Z"),
                    90
            ));
        }
    }
}
