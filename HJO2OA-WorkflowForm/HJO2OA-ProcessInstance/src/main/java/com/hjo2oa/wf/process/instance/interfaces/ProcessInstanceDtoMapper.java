package com.hjo2oa.wf.process.instance.interfaces;

import com.hjo2oa.wf.process.instance.domain.ProcessInstanceViews;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceDtoMapper {

    public ProcessInstanceDtos.InstanceDetailResponse toDetailResponse(
            ProcessInstanceViews.InstanceDetailView view
    ) {
        return new ProcessInstanceDtos.InstanceDetailResponse(
                toProcessResponse(view.instance()),
                view.tasks().stream().map(this::toTaskResponse).toList(),
                view.actions().stream().map(this::toActionResponse).toList(),
                view.nodeHistory().stream().map(this::toNodeHistoryResponse).toList(),
                view.variableHistory().stream().map(this::toVariableHistoryResponse).toList()
        );
    }

    public ProcessInstanceDtos.TaskInstanceResponse toTaskResponse(
            ProcessInstanceViews.TaskInstanceView view
    ) {
        return new ProcessInstanceDtos.TaskInstanceResponse(
                view.id(),
                view.instanceId(),
                view.nodeId(),
                view.nodeName(),
                view.nodeType(),
                view.assigneeId(),
                view.assigneeOrgId(),
                view.assigneeDeptId(),
                view.assigneePositionId(),
                view.candidateType(),
                view.candidateIds(),
                view.multiInstanceType(),
                view.completionCondition(),
                view.status(),
                view.claimTime(),
                view.completedTime(),
                view.dueTime(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private ProcessInstanceDtos.ProcessInstanceResponse toProcessResponse(
            ProcessInstanceViews.ProcessInstanceView view
    ) {
        return new ProcessInstanceDtos.ProcessInstanceResponse(
                view.id(),
                view.definitionId(),
                view.definitionVersion(),
                view.definitionCode(),
                view.businessKey(),
                view.title(),
                view.category(),
                view.initiatorId(),
                view.initiatorOrgId(),
                view.initiatorDeptId(),
                view.initiatorPositionId(),
                view.formMetadataId(),
                view.formDataId(),
                view.currentNodes(),
                view.status(),
                view.startTime(),
                view.endTime(),
                view.tenantId(),
                view.idempotencyKey(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private ProcessInstanceDtos.TaskActionResponse toActionResponse(
            ProcessInstanceViews.TaskActionView view
    ) {
        return new ProcessInstanceDtos.TaskActionResponse(
                view.id(),
                view.taskId(),
                view.instanceId(),
                view.actionCode(),
                view.actionName(),
                view.operatorId(),
                view.operatorOrgId(),
                view.operatorPositionId(),
                view.opinion(),
                view.targetNodeId(),
                view.formDataPatch(),
                view.createdAt()
        );
    }

    private ProcessInstanceDtos.NodeHistoryResponse toNodeHistoryResponse(
            ProcessInstanceViews.NodeHistoryView view
    ) {
        return new ProcessInstanceDtos.NodeHistoryResponse(
                view.id(),
                view.instanceId(),
                view.taskId(),
                view.nodeId(),
                view.nodeName(),
                view.nodeType(),
                view.status(),
                view.actionCode(),
                view.operatorId(),
                view.occurredAt(),
                view.tenantId()
        );
    }

    private ProcessInstanceDtos.VariableHistoryResponse toVariableHistoryResponse(
            ProcessInstanceViews.VariableHistoryView view
    ) {
        return new ProcessInstanceDtos.VariableHistoryResponse(
                view.id(),
                view.instanceId(),
                view.taskId(),
                view.variableName(),
                view.oldValue(),
                view.newValue(),
                view.operatorId(),
                view.occurredAt(),
                view.tenantId()
        );
    }
}
