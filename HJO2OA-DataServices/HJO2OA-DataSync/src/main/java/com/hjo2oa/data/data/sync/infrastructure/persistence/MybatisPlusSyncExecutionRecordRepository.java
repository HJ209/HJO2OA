package com.hjo2oa.data.data.sync.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.ReconciliationStatus;
import com.hjo2oa.data.data.sync.domain.SyncDiffSummary;
import com.hjo2oa.data.data.sync.domain.SyncDifferenceItem;
import com.hjo2oa.data.data.sync.domain.SyncExecutionFilter;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecord;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecordRepository;
import com.hjo2oa.data.data.sync.domain.SyncResultSummary;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisPlusSyncExecutionRecordRepository implements SyncExecutionRecordRepository {

    private static final TypeReference<List<SyncDifferenceItem>> DIFFERENCE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SyncExecutionRecordMapper executionRecordMapper;
    private final DataSyncJsonCodec jsonCodec;

    public MybatisPlusSyncExecutionRecordRepository(
            SyncExecutionRecordMapper executionRecordMapper,
            DataSyncJsonCodec jsonCodec
    ) {
        this.executionRecordMapper = Objects.requireNonNull(
                executionRecordMapper,
                "executionRecordMapper must not be null"
        );
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec must not be null");
    }

    @Override
    public Optional<SyncExecutionRecord> findById(UUID executionId) {
        return Optional.ofNullable(executionRecordMapper.selectById(executionId)).map(this::toDomain);
    }

    @Override
    public Optional<SyncExecutionRecord> findByTaskIdAndIdempotencyKey(UUID taskId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        LambdaQueryWrapper<SyncExecutionRecordDO> wrapper = new LambdaQueryWrapper<SyncExecutionRecordDO>()
                .eq(SyncExecutionRecordDO::getSyncTaskId, taskId)
                .eq(SyncExecutionRecordDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(SyncExecutionRecordDO::getStartedAt);
        return executionRecordMapper.selectList(wrapper).stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<SyncExecutionRecord> findLatestByTaskId(UUID taskId) {
        LambdaQueryWrapper<SyncExecutionRecordDO> wrapper = new LambdaQueryWrapper<SyncExecutionRecordDO>()
                .eq(SyncExecutionRecordDO::getSyncTaskId, taskId)
                .orderByDesc(SyncExecutionRecordDO::getStartedAt)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        return Optional.ofNullable(executionRecordMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Optional<SyncExecutionRecord> findLatestSuccessfulByTaskId(UUID taskId) {
        LambdaQueryWrapper<SyncExecutionRecordDO> wrapper = new LambdaQueryWrapper<SyncExecutionRecordDO>()
                .eq(SyncExecutionRecordDO::getSyncTaskId, taskId)
                .eq(SyncExecutionRecordDO::getExecutionStatus, ExecutionStatus.SUCCESS.name())
                .orderByDesc(SyncExecutionRecordDO::getStartedAt)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        return Optional.ofNullable(executionRecordMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public PagedResult<SyncExecutionRecord> page(SyncExecutionFilter filter) {
        LambdaQueryWrapper<SyncExecutionRecordDO> wrapper = new LambdaQueryWrapper<SyncExecutionRecordDO>()
                .eq(filter.taskId() != null, SyncExecutionRecordDO::getSyncTaskId, filter.taskId())
                .like(filter.taskCode() != null, SyncExecutionRecordDO::getTaskCode, filter.taskCode())
                .eq(filter.executionStatus() != null, SyncExecutionRecordDO::getExecutionStatus, enumName(filter.executionStatus()))
                .eq(filter.triggerType() != null, SyncExecutionRecordDO::getTriggerType, enumName(filter.triggerType()))
                .ge(filter.startedFrom() != null, SyncExecutionRecordDO::getStartedAt, filter.startedFrom())
                .le(filter.startedTo() != null, SyncExecutionRecordDO::getStartedAt, filter.startedTo())
                .orderByDesc(SyncExecutionRecordDO::getStartedAt);
        Page<SyncExecutionRecordDO> page = executionRecordMapper.selectPage(new Page<>(filter.page(), filter.size()), wrapper);
        List<SyncExecutionRecord> items = page.getRecords().stream().map(this::toDomain).toList();
        return new PagedResult<>(items, (int) page.getCurrent(), (int) page.getSize(), page.getTotal());
    }

    @Override
    public void save(SyncExecutionRecord executionRecord) {
        SyncExecutionRecordDO executionRecordDO = toDO(executionRecord);
        if (executionRecordMapper.selectById(executionRecord.executionId()) == null) {
            executionRecordMapper.insert(executionRecordDO);
        } else {
            executionRecordMapper.updateById(executionRecordDO);
        }
    }

    private SyncExecutionRecord toDomain(SyncExecutionRecordDO executionDO) {
        return new SyncExecutionRecord(
                executionDO.getId(),
                executionDO.getSyncTaskId(),
                executionDO.getTenantId(),
                executionDO.getTaskCode(),
                executionDO.getParentExecutionId(),
                executionDO.getExecutionBatchNo(),
                ExecutionTriggerType.valueOf(executionDO.getTriggerType()),
                ExecutionStatus.valueOf(executionDO.getExecutionStatus()),
                executionDO.getIdempotencyKey(),
                executionDO.getCheckpointValue(),
                executionDO.getRetryCount() == null ? 0 : executionDO.getRetryCount(),
                Boolean.TRUE.equals(executionDO.getRetryable()),
                optional(executionDO.getResultSummaryJson(), SyncResultSummary.class, SyncResultSummary.empty()),
                optional(executionDO.getDiffSummaryJson(), SyncDiffSummary.class, SyncDiffSummary.clean()),
                optional(executionDO.getDifferenceDetailsJson(), DIFFERENCE_LIST_TYPE, List.of()),
                optional(executionDO.getTriggerContextJson(), MAP_TYPE, Map.of()),
                executionDO.getFailureCode(),
                executionDO.getFailureMessage(),
                optional(executionDO.getReconciliationStatus(), ReconciliationStatus.class, ReconciliationStatus.NOT_CHECKED),
                executionDO.getOperatorAccountId(),
                executionDO.getOperatorPersonId(),
                executionDO.getStartedAt(),
                executionDO.getFinishedAt(),
                executionDO.getCreatedAt(),
                executionDO.getUpdatedAt()
        );
    }

    private SyncExecutionRecordDO toDO(SyncExecutionRecord record) {
        SyncExecutionRecordDO executionDO = new SyncExecutionRecordDO();
        executionDO.setId(record.executionId());
        executionDO.setTenantId(record.tenantId());
        executionDO.setSyncTaskId(record.syncTaskId());
        executionDO.setTaskCode(record.taskCode());
        executionDO.setParentExecutionId(record.parentExecutionId());
        executionDO.setExecutionBatchNo(record.executionBatchNo());
        executionDO.setTriggerType(enumName(record.triggerType()));
        executionDO.setExecutionStatus(enumName(record.executionStatus()));
        executionDO.setIdempotencyKey(record.idempotencyKey());
        executionDO.setCheckpointValue(record.checkpointValue());
        executionDO.setRetryCount(record.retryCount());
        executionDO.setRetryable(record.retryable());
        executionDO.setResultSummaryJson(jsonCodec.write(record.resultSummary()));
        executionDO.setDiffSummaryJson(jsonCodec.write(record.diffSummary()));
        executionDO.setDifferenceDetailsJson(jsonCodec.write(record.differenceItems()));
        executionDO.setTriggerContextJson(jsonCodec.write(record.triggerContext()));
        executionDO.setFailureCode(record.failureCode());
        executionDO.setFailureMessage(record.failureMessage());
        executionDO.setReconciliationStatus(record.reconciliationStatus() == null ? null : record.reconciliationStatus().name());
        executionDO.setOperatorAccountId(record.operatorAccountId());
        executionDO.setOperatorPersonId(record.operatorPersonId());
        executionDO.setStartedAt(record.startedAt());
        executionDO.setFinishedAt(record.finishedAt());
        executionDO.setCreatedAt(record.createdAt());
        executionDO.setUpdatedAt(record.updatedAt());
        executionDO.setDeleted(0);
        return executionDO;
    }

    private <T> T optional(String json, Class<T> targetType, T defaultValue) {
        T value = jsonCodec.read(json, targetType);
        return value == null ? defaultValue : value;
    }

    private <T> T optional(String json, TypeReference<T> typeReference, T defaultValue) {
        T value = jsonCodec.read(json, typeReference);
        return value == null ? defaultValue : value;
    }

    private ReconciliationStatus optional(String rawStatus, Class<ReconciliationStatus> targetType, ReconciliationStatus defaultValue) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return defaultValue;
        }
        return ReconciliationStatus.valueOf(rawStatus);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
