package com.hjo2oa.process.monitor.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.ExceptionProcessInstanceView;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.MonitoredProcessInstanceView;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.NodeTrailView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessInterventionCommand;
import com.hjo2oa.process.monitor.domain.ProcessInterventionView;
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
        assertThat(service.instances(filter, "running")).hasSize(1);
        assertThat(service.exceptionInstances(filter)).hasSize(1);
        assertThat(service.nodeTrail(UUID.randomUUID(), UUID.randomUUID())).hasSize(1);
    }

    @Test
    void shouldApplyAdministratorInterventionAndRecordAuditTrail() {
        UUID tenantId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID targetAssigneeId = UUID.randomUUID();

        ProcessInterventionView intervention = service.intervene(new ProcessInterventionCommand(
                tenantId,
                instanceId,
                taskId,
                "reassign_task",
                UUID.randomUUID(),
                targetAssigneeId,
                "rebalance workload"
        ));

        assertThat(repository.reassignedTaskId).isEqualTo(taskId);
        assertThat(repository.reassignedAssigneeId).isEqualTo(targetAssigneeId);
        assertThat(intervention.actionType()).isEqualTo("REASSIGN_TASK");
    }

    private static final class CapturingRepository implements ProcessMonitorQueryRepository {

        private MonitorQueryFilter capturedFilter;
        private UUID reassignedTaskId;
        private UUID reassignedAssigneeId;

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

        @Override
        public List<MonitoredProcessInstanceView> findInstances(MonitorQueryFilter filter, String status) {
            return List.of(new MonitoredProcessInstanceView(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "expense",
                    "Expense approval",
                    "finance",
                    UUID.randomUUID(),
                    status,
                    Instant.parse("2026-01-01T00:00:00Z"),
                    null,
                    Instant.parse("2026-01-02T00:00:00Z")
            ));
        }

        @Override
        public List<ExceptionProcessInstanceView> findExceptionInstances(MonitorQueryFilter filter) {
            return List.of(new ExceptionProcessInstanceView(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "expense",
                    "Expense approval",
                    "finance",
                    "RUNNING",
                    "OVERDUE_TASK",
                    90,
                    Instant.parse("2026-01-02T00:00:00Z")
            ));
        }

        @Override
        public List<NodeTrailView> findNodeTrail(UUID tenantId, UUID instanceId) {
            return List.of(new NodeTrailView(
                    UUID.randomUUID(),
                    instanceId,
                    "managerApprove",
                    "Manager approve",
                    "USER_TASK",
                    UUID.randomUUID(),
                    "CLAIMED",
                    Instant.parse("2026-01-01T00:00:00Z"),
                    null,
                    null,
                    null,
                    "APPROVE",
                    "Approve",
                    UUID.randomUUID(),
                    Instant.parse("2026-01-01T02:00:00Z")
            ));
        }

        @Override
        public List<ProcessInterventionView> findInterventions(UUID tenantId, UUID instanceId) {
            return List.of(new ProcessInterventionView(
                    UUID.randomUUID(),
                    instanceId,
                    null,
                    "SUSPEND",
                    UUID.randomUUID(),
                    null,
                    "maintenance",
                    Instant.parse("2026-01-02T00:00:00Z")
            ));
        }

        @Override
        public ProcessInterventionView recordIntervention(ProcessInterventionCommand command) {
            return new ProcessInterventionView(
                    UUID.randomUUID(),
                    command.instanceId(),
                    command.taskId(),
                    command.actionType(),
                    command.operatorId(),
                    command.targetAssigneeId(),
                    command.reason(),
                    Instant.parse("2026-01-02T00:00:00Z")
            );
        }

        @Override
        public void suspendInstance(UUID tenantId, UUID instanceId) {
        }

        @Override
        public void resumeInstance(UUID tenantId, UUID instanceId) {
        }

        @Override
        public void terminateInstance(UUID tenantId, UUID instanceId) {
        }

        @Override
        public void reassignTask(UUID tenantId, UUID taskId, UUID targetAssigneeId) {
            reassignedTaskId = taskId;
            reassignedAssigneeId = targetAssigneeId;
        }
    }
}
