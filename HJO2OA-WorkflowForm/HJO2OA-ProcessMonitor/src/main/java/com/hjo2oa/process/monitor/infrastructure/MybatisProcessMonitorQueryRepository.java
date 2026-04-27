package com.hjo2oa.process.monitor.infrastructure;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessMonitorQueryRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisProcessMonitorQueryRepository implements ProcessMonitorQueryRepository {

    private final ProcessMonitorQueryMapper mapper;

    public MybatisProcessMonitorQueryRepository(ProcessMonitorQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter) {
        return mapper.analyzeProcessDurations(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter) {
        return mapper.findStalledNodes(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter) {
        return mapper.rankApprovalCongestion(filter).stream().map(this::toView).toList();
    }

    @Override
    public List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter) {
        return mapper.findOverdueTasks(filter).stream().map(this::toView).toList();
    }

    private ProcessDurationAnalysisView toView(ProcessDurationAnalysisRow row) {
        return new ProcessDurationAnalysisView(
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                value(row.getInstanceCount()),
                value(row.getCompletedCount()),
                value(row.getRunningCount()),
                value(row.getAverageDurationMinutes()),
                value(row.getMaxDurationMinutes())
        );
    }

    private NodeStagnationAnalysisView toView(NodeStagnationAnalysisRow row) {
        return new NodeStagnationAnalysisView(
                row.getTaskId(),
                row.getInstanceId(),
                row.getInstanceTitle(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                row.getNodeId(),
                row.getNodeName(),
                row.getAssigneeId(),
                row.getTaskStatus(),
                row.getTaskCreatedAt(),
                row.getDueTime(),
                value(row.getStalledMinutes())
        );
    }

    private ApprovalCongestionAnalysisView toView(ApprovalCongestionAnalysisRow row) {
        return new ApprovalCongestionAnalysisView(
                row.getAssigneeId(),
                value(row.getPendingCount()),
                value(row.getOverdueCount()),
                row.getOldestPendingAt(),
                row.getNearestDueTime()
        );
    }

    private OverdueTaskObservationView toView(OverdueTaskObservationRow row) {
        return new OverdueTaskObservationView(
                row.getTaskId(),
                row.getInstanceId(),
                row.getInstanceTitle(),
                row.getDefinitionId(),
                row.getDefinitionCode(),
                row.getCategory(),
                row.getNodeId(),
                row.getNodeName(),
                row.getAssigneeId(),
                row.getDueTime(),
                value(row.getOverdueMinutes())
        );
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}
