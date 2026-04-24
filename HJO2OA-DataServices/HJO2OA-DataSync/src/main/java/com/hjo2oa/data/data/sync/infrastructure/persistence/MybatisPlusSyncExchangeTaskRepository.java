package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConnectorDependencyStatus;
import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.domain.SyncMappingRule;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTaskFilter;
import com.hjo2oa.data.data.sync.domain.SyncTaskStatus;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisPlusSyncExchangeTaskRepository implements SyncExchangeTaskRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SyncExchangeTaskMapper taskMapper;
    private final SyncMappingRuleMapper mappingRuleMapper;
    private final DataSyncJsonCodec jsonCodec;

    public MybatisPlusSyncExchangeTaskRepository(
            SyncExchangeTaskMapper taskMapper,
            SyncMappingRuleMapper mappingRuleMapper,
            DataSyncJsonCodec jsonCodec
    ) {
        this.taskMapper = Objects.requireNonNull(taskMapper, "taskMapper must not be null");
        this.mappingRuleMapper = Objects.requireNonNull(mappingRuleMapper, "mappingRuleMapper must not be null");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec must not be null");
    }

    @Override
    public Optional<SyncExchangeTask> findById(UUID taskId) {
        SyncExchangeTaskDO taskDO = taskMapper.selectById(taskId);
        if (taskDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(taskDO, loadMappings(List.of(taskId)).getOrDefault(taskId, List.of())));
    }

    @Override
    public Optional<SyncExchangeTask> findByCode(UUID tenantId, String code) {
        LambdaQueryWrapper<SyncExchangeTaskDO> wrapper = new LambdaQueryWrapper<SyncExchangeTaskDO>()
                .eq(SyncExchangeTaskDO::getTenantId, tenantId)
                .eq(SyncExchangeTaskDO::getCode, code);
        SyncExchangeTaskDO taskDO = taskMapper.selectOne(wrapper);
        if (taskDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(
                taskDO,
                loadMappings(List.of(taskDO.getId())).getOrDefault(taskDO.getId(), List.of())
        ));
    }

    @Override
    public PagedResult<SyncExchangeTask> page(SyncTaskFilter filter) {
        LambdaQueryWrapper<SyncExchangeTaskDO> wrapper = new LambdaQueryWrapper<SyncExchangeTaskDO>()
                .eq(filter.tenantId() != null, SyncExchangeTaskDO::getTenantId, filter.tenantId())
                .like(filter.code() != null, SyncExchangeTaskDO::getCode, filter.code())
                .eq(filter.syncMode() != null, SyncExchangeTaskDO::getSyncMode, enumName(filter.syncMode()))
                .eq(filter.status() != null, SyncExchangeTaskDO::getStatus, enumName(filter.status()))
                .eq(filter.sourceConnectorId() != null, SyncExchangeTaskDO::getSourceConnectorId, filter.sourceConnectorId())
                .eq(filter.targetConnectorId() != null, SyncExchangeTaskDO::getTargetConnectorId, filter.targetConnectorId())
                .orderByDesc(SyncExchangeTaskDO::getUpdatedAt);
        Page<SyncExchangeTaskDO> page = taskMapper.selectPage(new Page<>(filter.page(), filter.size()), wrapper);
        List<UUID> taskIds = page.getRecords().stream().map(SyncExchangeTaskDO::getId).toList();
        Map<UUID, List<SyncMappingRule>> mappings = loadMappings(taskIds);
        List<SyncExchangeTask> items = page.getRecords().stream()
                .map(record -> toDomain(record, mappings.getOrDefault(record.getId(), List.of())))
                .toList();
        return new PagedResult<>(items, (int) page.getCurrent(), (int) page.getSize(), page.getTotal());
    }

    @Override
    public List<SyncExchangeTask> findByConnectorId(UUID connectorId) {
        LambdaQueryWrapper<SyncExchangeTaskDO> wrapper = new LambdaQueryWrapper<SyncExchangeTaskDO>()
                .and(query -> query.eq(SyncExchangeTaskDO::getSourceConnectorId, connectorId)
                        .or()
                        .eq(SyncExchangeTaskDO::getTargetConnectorId, connectorId));
        List<SyncExchangeTaskDO> records = taskMapper.selectList(wrapper);
        Map<UUID, List<SyncMappingRule>> mappings = loadMappings(records.stream().map(SyncExchangeTaskDO::getId).toList());
        return records.stream().map(record -> toDomain(record, mappings.getOrDefault(record.getId(), List.of()))).toList();
    }

    @Override
    public List<SyncExchangeTask> findActiveEventDrivenTasks() {
        LambdaQueryWrapper<SyncExchangeTaskDO> wrapper = new LambdaQueryWrapper<SyncExchangeTaskDO>()
                .eq(SyncExchangeTaskDO::getStatus, enumName(SyncTaskStatus.ACTIVE))
                .eq(SyncExchangeTaskDO::getSyncMode, enumName(SyncMode.EVENT_DRIVEN))
                .orderByDesc(SyncExchangeTaskDO::getUpdatedAt);
        List<SyncExchangeTaskDO> records = taskMapper.selectList(wrapper);
        Map<UUID, List<SyncMappingRule>> mappings = loadMappings(records.stream().map(SyncExchangeTaskDO::getId).toList());
        return records.stream().map(record -> toDomain(record, mappings.getOrDefault(record.getId(), List.of()))).toList();
    }

    @Override
    public List<SyncExchangeTask> findActiveScheduledTasks() {
        LambdaQueryWrapper<SyncExchangeTaskDO> wrapper = new LambdaQueryWrapper<SyncExchangeTaskDO>()
                .eq(SyncExchangeTaskDO::getStatus, enumName(SyncTaskStatus.ACTIVE))
                .orderByDesc(SyncExchangeTaskDO::getUpdatedAt);
        List<SyncExchangeTaskDO> records = taskMapper.selectList(wrapper);
        Map<UUID, List<SyncMappingRule>> mappings = loadMappings(records.stream().map(SyncExchangeTaskDO::getId).toList());
        return records.stream()
                .map(record -> toDomain(record, mappings.getOrDefault(record.getId(), List.of())))
                .filter(task -> task.scheduleConfig().enabled() || task.triggerConfig().schedulerJobCode() != null)
                .toList();
    }

    @Override
    public void save(SyncExchangeTask task) {
        SyncExchangeTaskDO taskDO = toDO(task);
        if (taskMapper.selectById(task.taskId()) == null) {
            taskMapper.insert(taskDO);
        } else {
            taskMapper.updateById(taskDO);
        }
        LambdaQueryWrapper<SyncMappingRuleDO> deleteWrapper = new LambdaQueryWrapper<SyncMappingRuleDO>()
                .eq(SyncMappingRuleDO::getSyncTaskId, task.taskId());
        mappingRuleMapper.delete(deleteWrapper);
        for (SyncMappingRule rule : task.mappingRules()) {
            mappingRuleMapper.insert(toDO(rule, task.tenantId()));
        }
    }

    @Override
    public void delete(UUID taskId) {
        LambdaQueryWrapper<SyncMappingRuleDO> deleteWrapper = new LambdaQueryWrapper<SyncMappingRuleDO>()
                .eq(SyncMappingRuleDO::getSyncTaskId, taskId);
        mappingRuleMapper.delete(deleteWrapper);
        taskMapper.deleteById(taskId);
    }

    private Map<UUID, List<SyncMappingRule>> loadMappings(List<UUID> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SyncMappingRuleDO> wrapper = new LambdaQueryWrapper<SyncMappingRuleDO>()
                .in(SyncMappingRuleDO::getSyncTaskId, taskIds)
                .orderByAsc(SyncMappingRuleDO::getSortOrder)
                .orderByAsc(SyncMappingRuleDO::getCreatedAt);
        Map<UUID, List<SyncMappingRule>> mappings = new LinkedHashMap<>();
        for (SyncMappingRuleDO ruleDO : mappingRuleMapper.selectList(wrapper)) {
            mappings.computeIfAbsent(ruleDO.getSyncTaskId(), ignored -> new ArrayList<>()).add(toDomain(ruleDO));
        }
        mappings.values().forEach(list -> list.sort(Comparator.comparingInt(SyncMappingRule::sortOrder)));
        return mappings;
    }

    private SyncExchangeTask toDomain(SyncExchangeTaskDO taskDO, List<SyncMappingRule> mappings) {
        return new SyncExchangeTask(
                taskDO.getId(),
                taskDO.getTenantId(),
                taskDO.getCode(),
                taskDO.getName(),
                taskDO.getDescription(),
                SyncTaskType.valueOf(taskDO.getTaskType()),
                SyncMode.valueOf(taskDO.getSyncMode()),
                taskDO.getSourceConnectorId(),
                taskDO.getTargetConnectorId(),
                ConnectorDependencyStatus.valueOf(taskDO.getDependencyStatus()),
                CheckpointMode.valueOf(taskDO.getCheckpointMode()),
                optional(taskDO.getCheckpointConfigJson(), SyncCheckpointConfig.class, SyncCheckpointConfig.empty()),
                optional(taskDO.getTriggerConfigJson(), SyncTriggerConfig.class, SyncTriggerConfig.manualOnly()),
                optional(taskDO.getRetryPolicyJson(), SyncRetryPolicy.class, SyncRetryPolicy.manualOnly()),
                optional(taskDO.getCompensationPolicyJson(), SyncCompensationPolicy.class, SyncCompensationPolicy.manualDefault()),
                optional(taskDO.getReconciliationPolicyJson(), SyncReconciliationPolicy.class, SyncReconciliationPolicy.enabledByDefault()),
                optional(taskDO.getScheduleConfigJson(), SyncScheduleConfig.class, SyncScheduleConfig.disabled()),
                SyncTaskStatus.valueOf(taskDO.getStatus()),
                mappings,
                taskDO.getCreatedAt(),
                taskDO.getUpdatedAt()
        );
    }

    private SyncMappingRule toDomain(SyncMappingRuleDO ruleDO) {
        Map<String, Object> transformRule = optional(ruleDO.getTransformRuleJson(), MAP_TYPE, Map.of());
        return new SyncMappingRule(
                ruleDO.getId(),
                ruleDO.getSyncTaskId(),
                ruleDO.getSourceField(),
                ruleDO.getTargetField(),
                transformRule,
                com.hjo2oa.data.data.sync.domain.ConflictStrategy.valueOf(ruleDO.getConflictStrategy()),
                Boolean.TRUE.equals(ruleDO.getKeyMapping()),
                ruleDO.getSortOrder() == null ? 0 : ruleDO.getSortOrder(),
                ruleDO.getCreatedAt(),
                ruleDO.getUpdatedAt()
        );
    }

    private SyncExchangeTaskDO toDO(SyncExchangeTask task) {
        SyncExchangeTaskDO taskDO = new SyncExchangeTaskDO();
        taskDO.setId(task.taskId());
        taskDO.setTenantId(task.tenantId());
        taskDO.setCode(task.code());
        taskDO.setName(task.name());
        taskDO.setDescription(task.description());
        taskDO.setTaskType(enumName(task.taskType()));
        taskDO.setSyncMode(enumName(task.syncMode()));
        taskDO.setSourceConnectorId(task.sourceConnectorId());
        taskDO.setTargetConnectorId(task.targetConnectorId());
        taskDO.setDependencyStatus(enumName(task.dependencyStatus()));
        taskDO.setCheckpointMode(enumName(task.checkpointMode()));
        taskDO.setCheckpointConfigJson(jsonCodec.write(task.checkpointConfig()));
        taskDO.setTriggerConfigJson(jsonCodec.write(task.triggerConfig()));
        taskDO.setRetryPolicyJson(jsonCodec.write(task.retryPolicy()));
        taskDO.setCompensationPolicyJson(jsonCodec.write(task.compensationPolicy()));
        taskDO.setReconciliationPolicyJson(jsonCodec.write(task.reconciliationPolicy()));
        taskDO.setScheduleConfigJson(jsonCodec.write(task.scheduleConfig()));
        taskDO.setStatus(enumName(task.status()));
        taskDO.setCreatedAt(task.createdAt());
        taskDO.setUpdatedAt(task.updatedAt());
        taskDO.setDeleted(0);
        return taskDO;
    }

    private SyncMappingRuleDO toDO(SyncMappingRule rule, UUID tenantId) {
        SyncMappingRuleDO ruleDO = new SyncMappingRuleDO();
        ruleDO.setId(rule.ruleId());
        ruleDO.setTenantId(tenantId);
        ruleDO.setSyncTaskId(rule.syncTaskId());
        ruleDO.setSourceField(rule.sourceField());
        ruleDO.setTargetField(rule.targetField());
        ruleDO.setTransformRuleJson(jsonCodec.write(rule.transformRule()));
        ruleDO.setConflictStrategy(enumName(rule.conflictStrategy()));
        ruleDO.setKeyMapping(rule.keyMapping());
        ruleDO.setSortOrder(rule.sortOrder());
        ruleDO.setCreatedAt(rule.createdAt());
        ruleDO.setUpdatedAt(rule.updatedAt());
        ruleDO.setDeleted(0);
        return ruleDO;
    }

    private <T> T optional(String json, Class<T> targetType, T defaultValue) {
        T value = jsonCodec.read(json, targetType);
        return value == null ? defaultValue : value;
    }

    private <T> T optional(String json, TypeReference<T> typeReference, T defaultValue) {
        T value = jsonCodec.read(json, typeReference);
        return value == null ? defaultValue : value;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
