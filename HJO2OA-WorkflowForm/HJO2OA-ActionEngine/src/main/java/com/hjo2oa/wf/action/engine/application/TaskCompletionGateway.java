package com.hjo2oa.wf.action.engine.application;

import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionRequest;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;

public interface TaskCompletionGateway {

    void apply(TaskInstanceSnapshot task, ActionDefinition definition, ActionExecutionRequest request, TaskStatus status);

    static TaskCompletionGateway noop() {
        return NoopTaskCompletionGateway.INSTANCE;
    }

    enum NoopTaskCompletionGateway implements TaskCompletionGateway {
        INSTANCE;

        @Override
        public void apply(
                TaskInstanceSnapshot task,
                ActionDefinition definition,
                ActionExecutionRequest request,
                TaskStatus status
        ) {
        }
    }
}
