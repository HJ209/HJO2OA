package com.hjo2oa.process.monitor.application;

import com.hjo2oa.process.monitor.domain.ApprovalCongestionAnalysisView;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.NodeStagnationAnalysisView;
import com.hjo2oa.process.monitor.domain.OverdueTaskObservationView;
import com.hjo2oa.process.monitor.domain.ProcessDurationAnalysisView;
import com.hjo2oa.process.monitor.domain.ProcessMonitorQueryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessMonitorQueryApplicationService {

    private final ProcessMonitorQueryRepository queryRepository;

    public ProcessMonitorQueryApplicationService(ProcessMonitorQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public List<ProcessDurationAnalysisView> processDurations(MonitorQueryFilter filter) {
        return queryRepository.analyzeProcessDurations(filter);
    }

    public List<NodeStagnationAnalysisView> stalledNodes(MonitorQueryFilter filter) {
        return queryRepository.findStalledNodes(filter);
    }

    public List<ApprovalCongestionAnalysisView> approvalCongestion(MonitorQueryFilter filter) {
        return queryRepository.rankApprovalCongestion(filter);
    }

    public List<OverdueTaskObservationView> overdueTasks(MonitorQueryFilter filter) {
        return queryRepository.findOverdueTasks(filter);
    }
}
