package com.hjo2oa.process.monitor.application;

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
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

    public List<MonitoredProcessInstanceView> instances(MonitorQueryFilter filter, String status) {
        return queryRepository.findInstances(filter, normalize(status));
    }

    public List<ExceptionProcessInstanceView> exceptionInstances(MonitorQueryFilter filter) {
        return queryRepository.findExceptionInstances(filter);
    }

    public List<NodeTrailView> nodeTrail(UUID tenantId, UUID instanceId) {
        return queryRepository.findNodeTrail(tenantId, instanceId);
    }

    public List<ProcessInterventionView> interventions(UUID tenantId, UUID instanceId) {
        return queryRepository.findInterventions(tenantId, instanceId);
    }

    public ProcessInterventionView intervene(ProcessInterventionCommand command) {
        String actionType = normalize(command.actionType());
        if (actionType == null) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "actionType is required");
        }
        switch (actionType) {
            case "SUSPEND" -> queryRepository.suspendInstance(command.tenantId(), command.instanceId());
            case "RESUME" -> queryRepository.resumeInstance(command.tenantId(), command.instanceId());
            case "TERMINATE" -> queryRepository.terminateInstance(command.tenantId(), command.instanceId());
            case "REASSIGN_TASK" -> {
                if (command.taskId() == null || command.targetAssigneeId() == null) {
                    throw new BizException(
                            SharedErrorDescriptors.BAD_REQUEST,
                            "taskId and targetAssigneeId are required for REASSIGN_TASK"
                    );
                }
                queryRepository.reassignTask(command.tenantId(), command.taskId(), command.targetAssigneeId());
            }
            default -> throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Unsupported intervention actionType");
        }
        return queryRepository.recordIntervention(new ProcessInterventionCommand(
                command.tenantId(),
                command.instanceId(),
                command.taskId(),
                actionType,
                command.operatorId(),
                command.targetAssigneeId(),
                command.reason()
        ));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
