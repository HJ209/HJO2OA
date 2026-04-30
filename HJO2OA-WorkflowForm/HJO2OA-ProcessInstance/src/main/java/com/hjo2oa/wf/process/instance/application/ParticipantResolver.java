package com.hjo2oa.wf.process.instance.application;

import com.hjo2oa.wf.process.definition.domain.model.WorkflowParticipantRule;
import java.util.List;

public interface ParticipantResolver {

    List<ProcessInstanceCommands.TaskParticipantCommand> resolve(
            WorkflowParticipantRule rule,
            ParticipantResolutionContext context
    );
}
