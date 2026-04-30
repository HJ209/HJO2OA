package com.hjo2oa.wf.process.definition.infrastructure.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class FlowableServiceTaskPassThroughDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariableLocal("hjo2oaServiceTaskNodeId", execution.getCurrentActivityId());
        execution.setVariableLocal("hjo2oaServiceTaskPassedAt", java.time.Instant.now().toString());
    }
}
