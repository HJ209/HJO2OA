package com.hjo2oa.wf.action.engine.domain;

import java.util.List;
import java.util.Optional;

public interface ActionDefinitionRepository {

    List<ActionDefinition> findAvailableActions(TaskInstanceSnapshot task);

    Optional<ActionDefinition> findAvailableAction(TaskInstanceSnapshot task, String actionCode);
}
