package com.hjo2oa.process.monitor.application;

import static org.assertj.core.api.Assertions.assertThat;

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

class ProcessMonitorQueryApplicationServiceTest {

    private final CapturingRepository repository = new CapturingRepository();
    private final ProcessMonitorQueryApplicationService service =
            new ProcessMonitorQueryApplicationService(repository);

    @Test
    void delegatesDurationAnalysisWithNormalizedFilter() {
        UUID definitionId = UUID.randomUUID();
        MonitorQueryFilter filter = MonitorQueryFilter.of(
                null,
                definitionId,
                " expense ",
                " finance ",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                999,
                0L
        );

        List<ProcessDurationAnalysisView> result = service.processDurations(filter);

        assertThat(result).hasSize(1);
        assertThat(repository.capturedFilter.definitionId()).isEqualTo(definitionId);
        assertThat(repository.capturedFilter.definitionCode()).isEqualTo("expense");
        assertThat(repository.capturedFilter.category()).isEqualTo("finance");
        assertThat(repository.capturedFilter.limit()).isEqualTo(500);
        assertThat(repository.capturedFilter.stalledThresholdMinutes()).isEqualTo(1440);
    }

    @Test
    void exposesAllReadOnlyQueryViews() {
        MonitorQueryFilter filter = MonitorQueryFilter.of(null, null, null, null, null, null, 10, 60L);

        assertThat(service.stalledNodes(filter)).hasSize(1);
        assertThat(service.approvalCongestion(filter)).hasSize(1);
        assertThat(service.overdueTasks(filter)).hasSize(1);
    }

    private static final class CapturingRepository implements ProcessMonitorQueryRepository {

        private MonitorQueryFilter capturedFilter;

        @Override
        public List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter) {
            capturedFilter = filter;
            return List.of(new ProcessDurationAnalysisView(UUID.randomUUID(), "expense", "finance", 3, 2, 1, 42, 120));
        }

        @Override
        public List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter) {
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
                    Instant.parse("2026-01-01T00:00:00Z"),
                    null,
                    180
            ));
        }

        @Override
        public List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter) {
            return List.of(new ApprovalCongestionAnalysisView(
                    UUID.randomUUID(),
                    6,
                    2,
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-02T00:00:00Z")
            ));
        }

        @Override
        public List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter) {
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
                    Instant.parse("2026-01-02T00:00:00Z"),
                    90
            ));
        }
    }
}
