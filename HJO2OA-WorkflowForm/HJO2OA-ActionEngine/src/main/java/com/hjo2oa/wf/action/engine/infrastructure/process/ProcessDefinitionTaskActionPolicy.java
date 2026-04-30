package com.hjo2oa.wf.action.engine.infrastructure.process;

import com.hjo2oa.wf.action.engine.application.TaskActionPolicy;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionRepository;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowDefinitionJsonParser;
import com.hjo2oa.wf.process.definition.domain.model.WorkflowNodeDefinition;
import com.hjo2oa.wf.process.instance.domain.ProcessInstance;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceRepository;
import com.hjo2oa.wf.process.instance.domain.TaskInstance;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceRepository;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class ProcessDefinitionTaskActionPolicy implements TaskActionPolicy {

    private final TaskInstanceRepository taskRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final ProcessDefinitionRepository definitionRepository;
    private final WorkflowDefinitionJsonParser modelParser;

    public ProcessDefinitionTaskActionPolicy(
            TaskInstanceRepository taskRepository,
            ProcessInstanceRepository instanceRepository,
            ProcessDefinitionRepository definitionRepository,
            WorkflowDefinitionJsonParser modelParser
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.instanceRepository = Objects.requireNonNull(instanceRepository, "instanceRepository must not be null");
        this.definitionRepository = Objects.requireNonNull(definitionRepository, "definitionRepository must not be null");
        this.modelParser = Objects.requireNonNull(modelParser, "modelParser must not be null");
    }

    @Override
    public boolean isAllowed(TaskInstanceSnapshot taskSnapshot, String actionCode) {
        if (taskSnapshot == null || actionCode == null || actionCode.isBlank()) {
            return false;
        }
        TaskInstance task = taskRepository.findById(taskSnapshot.taskId()).orElse(null);
        if (task == null) {
            return false;
        }
        ProcessInstance instance = instanceRepository.findById(task.instanceId()).orElse(null);
        if (instance == null) {
            return false;
        }
        ProcessDefinition definition = definitionRepository.findById(instance.definitionId()).orElse(null);
        if (definition == null) {
            return false;
        }
        WorkflowNodeDefinition node = modelParser.parse(definition.nodes(), definition.routes())
                .findNode(task.nodeId())
                .orElse(null);
        if (node == null) {
            return false;
        }
        if (node.actionCodes() == null || node.actionCodes().isEmpty()) {
            return true;
        }
        String normalizedActionCode = actionCode.trim().toLowerCase(Locale.ROOT);
        return node.actionCodes().stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalizedActionCode::equals);
    }
}
