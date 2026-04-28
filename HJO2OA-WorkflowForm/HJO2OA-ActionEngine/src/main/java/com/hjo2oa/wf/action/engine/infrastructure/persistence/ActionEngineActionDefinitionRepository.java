package com.hjo2oa.wf.action.engine.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionDefinitionRepository;
import com.hjo2oa.wf.action.engine.domain.RouteTarget;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ActionEngineActionDefinitionRepository implements ActionDefinitionRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ActionEngineActionDefinitionMapper mapper;
    private final ObjectMapper objectMapper;

    public ActionEngineActionDefinitionRepository(ActionEngineActionDefinitionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ActionDefinition> findAvailableActions(TaskInstanceSnapshot task) {
        List<ActionDefinitionEntity> entities = mapper.selectList(new QueryWrapper<ActionDefinitionEntity>()
                .eq("tenant_id", task.tenantId())
                .orderByAsc("code"));
        if (entities.isEmpty()) {
            return presets(task.tenantId());
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ActionDefinition> findAvailableAction(TaskInstanceSnapshot task, String actionCode) {
        Optional<ActionDefinition> stored = Optional.ofNullable(mapper.selectOne(new QueryWrapper<ActionDefinitionEntity>()
                .eq("tenant_id", task.tenantId())
                .eq("code", actionCode))).map(this::toDomain);
        return stored.or(() -> presets(task.tenantId()).stream()
                .filter(action -> action.code().equals(actionCode))
                .findFirst());
    }

    private ActionDefinition toDomain(ActionDefinitionEntity entity) {
        return new ActionDefinition(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                ActionCategory.valueOf(entity.getCategory()),
                RouteTarget.valueOf(entity.getRouteTarget()),
                Boolean.TRUE.equals(entity.getRequireOpinion()),
                Boolean.TRUE.equals(entity.getRequireTarget()),
                readMap(entity.getUiConfigJson()),
                entity.getTenantId()
        );
    }

    private Map<String, Object> readMap(String json) {
        try {
            return json == null || json.isBlank() ? Map.of() : objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid action definition JSON", ex);
        }
    }

    private List<ActionDefinition> presets(String tenantId) {
        return List.of(
                ActionDefinition.preset("approve", "Approve", ActionCategory.APPROVE, RouteTarget.NEXT_NODE, false, false, tenantId),
                ActionDefinition.preset("reject", "Reject", ActionCategory.REJECT, RouteTarget.END, true, false, tenantId),
                ActionDefinition.preset("transfer", "Transfer", ActionCategory.TRANSFER, RouteTarget.CURRENT_NODE, false, true, tenantId),
                ActionDefinition.preset("add_sign", "Add sign", ActionCategory.ADD_SIGN, RouteTarget.CURRENT_NODE, false, true, tenantId),
                ActionDefinition.preset("reduce_sign", "Reduce sign", ActionCategory.REDUCE_SIGN, RouteTarget.CURRENT_NODE, false, true, tenantId)
        );
    }
}
