package com.hjo2oa.wf.action.engine.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionOperator;
import com.hjo2oa.wf.action.engine.domain.ActionResultStatus;
import com.hjo2oa.wf.action.engine.domain.TaskAction;
import com.hjo2oa.wf.action.engine.domain.TaskActionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class ActionEngineTaskActionRepository implements TaskActionRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ActionEngineTaskActionMapper mapper;
    private final ObjectMapper objectMapper;

    public ActionEngineTaskActionRepository(ActionEngineTaskActionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TaskAction> findByIdempotency(UUID taskId, String actionCode, String idempotencyKey) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<TaskActionEntity>()
                .eq("task_id", taskId)
                .eq("action_code", actionCode)
                .eq("idempotency_key", idempotencyKey))).map(this::toDomain);
    }

    @Override
    public TaskAction save(TaskAction taskAction) {
        TaskActionEntity existing = mapper.selectById(taskAction.id());
        TaskActionEntity entity = toEntity(taskAction, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(mapper.selectById(taskAction.id()));
    }

    @Override
    public List<TaskAction> findByTaskId(UUID taskId) {
        return mapper.selectList(new QueryWrapper<TaskActionEntity>()
                        .eq("task_id", taskId)
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TaskAction toDomain(TaskActionEntity entity) {
        return new TaskAction(
                entity.getId(),
                entity.getTaskId(),
                entity.getInstanceId(),
                entity.getActionCode(),
                ActionCategory.valueOf(entity.getCategory()),
                entity.getOpinion(),
                entity.getTargetNodeId(),
                readList(entity.getTargetAssigneeIdsJson()),
                readMap(entity.getFormDataPatchJson()),
                new ActionOperator(
                        entity.getOperatorAccountId(),
                        entity.getOperatorPersonId(),
                        entity.getOperatorPositionId(),
                        entity.getOperatorOrgId()
                ),
                entity.getIdempotencyKey(),
                ActionResultStatus.valueOf(entity.getResultStatus()),
                entity.getCreatedAt(),
                entity.getTenantId()
        );
    }

    private TaskActionEntity toEntity(TaskAction action, TaskActionEntity existing) {
        TaskActionEntity entity = existing == null ? new TaskActionEntity() : existing;
        entity.setId(action.id());
        entity.setTaskId(action.taskId());
        entity.setInstanceId(action.instanceId());
        entity.setActionCode(action.actionCode());
        entity.setCategory(action.category().name());
        entity.setOpinion(action.opinion());
        entity.setTargetNodeId(action.targetNodeId());
        entity.setTargetAssigneeIdsJson(write(action.targetAssigneeIds()));
        entity.setFormDataPatchJson(write(action.formDataPatch()));
        entity.setOperatorAccountId(action.operator().accountId());
        entity.setOperatorPersonId(action.operator().personId());
        entity.setOperatorPositionId(action.operator().positionId());
        entity.setOperatorOrgId(action.operator().orgId());
        entity.setIdempotencyKey(action.idempotencyKey());
        entity.setResultStatus(action.resultStatus().name());
        entity.setCreatedAt(action.createdAt());
        entity.setTenantId(action.tenantId());
        return entity;
    }

    private List<String> readList(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid task action assignee JSON", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return json == null || json.isBlank() ? Map.of() : objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid task action form data JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write task action JSON", ex);
        }
    }
}
