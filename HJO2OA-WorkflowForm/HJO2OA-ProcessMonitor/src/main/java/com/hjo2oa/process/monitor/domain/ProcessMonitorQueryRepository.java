package com.hjo2oa.process.monitor.domain;

import java.util.List;

public interface ProcessMonitorQueryRepository {

    List<ProcessDurationAnalysisView> analyzeProcessDurations(MonitorQueryFilter filter);

    List<NodeStagnationAnalysisView> findStalledNodes(MonitorQueryFilter filter);

    List<ApprovalCongestionAnalysisView> rankApprovalCongestion(MonitorQueryFilter filter);

    List<OverdueTaskObservationView> findOverdueTasks(MonitorQueryFilter filter);
}
