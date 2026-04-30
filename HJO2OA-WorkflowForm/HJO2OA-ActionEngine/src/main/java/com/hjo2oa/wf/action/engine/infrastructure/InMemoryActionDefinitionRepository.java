package com.hjo2oa.wf.action.engine.infrastructure;

import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionDefinitionRepository;
import com.hjo2oa.wf.action.engine.domain.RouteTarget;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryActionDefinitionRepository implements ActionDefinitionRepository {

    @Override
    public List<ActionDefinition> findAvailableActions(TaskInstanceSnapshot task) {
        return presets(task.tenantId());
    }

    @Override
    public Optional<ActionDefinition> findAvailableAction(TaskInstanceSnapshot task, String actionCode) {
        return presets(task.tenantId()).stream()
                .filter(action -> action.code().equals(actionCode))
                .findFirst();
    }

    private List<ActionDefinition> presets(String tenantId) {
        return List.of(
                ActionDefinition.preset(
                        "approve",
                        "Approve",
                        ActionCategory.APPROVE,
                        RouteTarget.NEXT_NODE,
                        false,
                        false,
                        tenantId
                ),
                ActionDefinition.preset(
                        "reject",
                        "Reject",
                        ActionCategory.REJECT,
                        RouteTarget.END,
                        true,
                        false,
                        tenantId
                ),
                ActionDefinition.preset(
                        "transfer",
                        "Transfer",
                        ActionCategory.TRANSFER,
                        RouteTarget.CURRENT_NODE,
                        false,
                        true,
                        tenantId
                ),
                ActionDefinition.preset(
                        "delegate",
                        "Delegate",
                        ActionCategory.DELEGATE,
                        RouteTarget.CURRENT_NODE,
                        false,
                        true,
                        tenantId
                ),
                ActionDefinition.preset(
                        "withdraw",
                        "Withdraw",
                        ActionCategory.WITHDRAW,
                        RouteTarget.END,
                        true,
                        false,
                        tenantId
                ),
                ActionDefinition.preset(
                        "add_sign",
                        "Add sign",
                        ActionCategory.ADD_SIGN,
                        RouteTarget.CURRENT_NODE,
                        false,
                        true,
                        tenantId
                ),
                ActionDefinition.preset(
                        "reduce_sign",
                        "Reduce sign",
                        ActionCategory.REDUCE_SIGN,
                        RouteTarget.CURRENT_NODE,
                        false,
                        true,
                        tenantId
                ),
                ActionDefinition.preset(
                        "terminate",
                        "Terminate",
                        ActionCategory.TERMINATE,
                        RouteTarget.END,
                        true,
                        false,
                        tenantId
                )
        );
    }
}
