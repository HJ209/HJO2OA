package com.hjo2oa.wf.process.instance.application;

import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import java.util.Map;

public interface ProcessInstanceEngineGateway {

    void start(ProcessInstance instance, ProcessInstanceCommands.StartProcessCommand command);

    void claim(TaskInstance task, ProcessInstanceCommands.ClaimTaskCommand command);

    void transfer(TaskInstance task, ProcessInstanceCommands.TransferTaskCommand command);

    void complete(
            ProcessInstance instance,
            TaskInstance task,
            ProcessInstanceCommands.CompleteTaskCommand command,
            Map<String, Object> variables
    );

    void terminate(ProcessInstance instance, ProcessInstanceCommands.TerminateProcessCommand command);

    static ProcessInstanceEngineGateway noop() {
        return NoopProcessInstanceEngineGateway.INSTANCE;
    }

    enum NoopProcessInstanceEngineGateway implements ProcessInstanceEngineGateway {
        INSTANCE;

        @Override
        public void start(ProcessInstance instance, ProcessInstanceCommands.StartProcessCommand command) {
        }

        @Override
        public void claim(TaskInstance task, ProcessInstanceCommands.ClaimTaskCommand command) {
        }

        @Override
        public void transfer(TaskInstance task, ProcessInstanceCommands.TransferTaskCommand command) {
        }

        @Override
        public void complete(
                ProcessInstance instance,
                TaskInstance task,
                ProcessInstanceCommands.CompleteTaskCommand command,
                Map<String, Object> variables
        ) {
        }

        @Override
        public void terminate(ProcessInstance instance, ProcessInstanceCommands.TerminateProcessCommand command) {
        }
    }
}
