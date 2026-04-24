package com.hjo2oa.data.connector.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjo2oa.data.connector.domain.ConnectorCheckType;
import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionRepository;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshot;
import com.hjo2oa.data.connector.domain.ConnectorHealthStatus;
import com.hjo2oa.data.connector.domain.ConnectorPageResult;
import com.hjo2oa.data.connector.domain.ConnectorParameter;
import com.hjo2oa.data.connector.domain.ConnectorStatus;
import com.hjo2oa.data.connector.domain.ConnectorType;
import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisConnectorDefinitionRepository implements ConnectorDefinitionRepository {

    private final ConnectorDefinitionMapper connectorDefinitionMapper;
    private final ConnectorParameterMapper connectorParameterMapper;
    private final ConnectorHealthSnapshotMapper connectorHealthSnapshotMapper;
    private final ConnectorJsonCodec connectorJsonCodec;

    public MybatisConnectorDefinitionRepository(
            ConnectorDefinitionMapper connectorDefinitionMapper,
            ConnectorParameterMapper connectorParameterMapper,
            ConnectorHealthSnapshotMapper connectorHealthSnapshotMapper,
            ConnectorJsonCodec connectorJsonCodec
    ) {
        this.connectorDefinitionMapper = connectorDefinitionMapper;
        this.connectorParameterMapper = connectorParameterMapper;
        this.connectorHealthSnapshotMapper = connectorHealthSnapshotMapper;
        this.connectorJsonCodec = connectorJsonCodec;
    }

    @Override
    public Optional<ConnectorDefinition> findById(String connectorId) {
        ConnectorDefinitionDO connectorDefinitionDO = connectorDefinitionMapper.selectById(connectorId);
        return Optional.ofNullable(connectorDefinitionDO).map(this::toDomain);
    }

    @Override
    public Optional<ConnectorDefinition> findByCode(String tenantId, String code) {
        LambdaQueryWrapper<ConnectorDefinitionDO> wrapper = new LambdaQueryWrapper<ConnectorDefinitionDO>()
                .eq(ConnectorDefinitionDO::getTenantId, tenantId)
                .eq(ConnectorDefinitionDO::getCode, code);
        return Optional.ofNullable(connectorDefinitionMapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public ConnectorPageResult findPage(
            String tenantId,
            ConnectorType connectorType,
            ConnectorStatus status,
            String code,
            String keyword,
            int page,
            int size
    ) {
        LambdaQueryWrapper<ConnectorDefinitionDO> wrapper = new LambdaQueryWrapper<ConnectorDefinitionDO>()
                .eq(ConnectorDefinitionDO::getTenantId, tenantId)
                .orderByAsc(ConnectorDefinitionDO::getCode);
        if (connectorType != null) {
            wrapper.eq(ConnectorDefinitionDO::getConnectorType, connectorType.name());
        }
        if (status != null) {
            wrapper.eq(ConnectorDefinitionDO::getStatus, status.name());
        }
        if (code != null && !code.isBlank()) {
            wrapper.eq(ConnectorDefinitionDO::getCode, code);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(ConnectorDefinitionDO::getCode, keyword)
                    .or()
                    .like(ConnectorDefinitionDO::getName, keyword));
        }
        Page<ConnectorDefinitionDO> definitionPage = connectorDefinitionMapper.selectPage(Page.of(page, size), wrapper);
        List<ConnectorDefinition> items = definitionPage.getRecords().stream().map(this::toDomain).toList();
        return new ConnectorPageResult(items, definitionPage.getTotal());
    }

    @Override
    public ConnectorDefinition save(ConnectorDefinition connectorDefinition) {
        ConnectorDefinitionDO definitionDO = toDefinitionDO(connectorDefinition);
        if (connectorDefinitionMapper.selectById(connectorDefinition.connectorId()) == null) {
            connectorDefinitionMapper.insert(definitionDO);
        } else {
            connectorDefinitionMapper.updateById(definitionDO);
        }
        connectorParameterMapper.delete(new LambdaQueryWrapper<ConnectorParameterDO>()
                .eq(ConnectorParameterDO::getConnectorId, connectorDefinition.connectorId()));
        for (ConnectorParameter parameter : connectorDefinition.parameters()) {
            connectorParameterMapper.insert(toParameterDO(parameter, connectorDefinition.updatedAt()));
        }
        return connectorDefinition;
    }

    @Override
    public ConnectorHealthSnapshot saveHealthSnapshot(ConnectorHealthSnapshot healthSnapshot) {
        ConnectorHealthSnapshotDO healthSnapshotDO = toHealthSnapshotDO(healthSnapshot);
        if (connectorHealthSnapshotMapper.selectById(healthSnapshot.snapshotId()) == null) {
            connectorHealthSnapshotMapper.insert(healthSnapshotDO);
        } else {
            connectorHealthSnapshotMapper.updateById(healthSnapshotDO);
        }
        return healthSnapshot;
    }

    @Override
    public Optional<ConnectorHealthSnapshot> findHealthSnapshotById(String snapshotId) {
        return Optional.ofNullable(connectorHealthSnapshotMapper.selectById(snapshotId)).map(this::toHealthSnapshot);
    }

    @Override
    public Optional<ConnectorHealthSnapshot> findLatestSnapshot(String connectorId, ConnectorCheckType checkType) {
        return findSnapshots(connectorId, checkType, null, null, null, 1).stream().findFirst();
    }

    @Override
    public List<ConnectorHealthSnapshot> findSnapshots(
            String connectorId,
            ConnectorCheckType checkType,
            ConnectorHealthStatus healthStatus,
            Instant checkedFrom,
            Instant checkedTo,
            int limit
    ) {
        LambdaQueryWrapper<ConnectorHealthSnapshotDO> wrapper = new LambdaQueryWrapper<ConnectorHealthSnapshotDO>()
                .eq(ConnectorHealthSnapshotDO::getConnectorId, connectorId)
                .orderByDesc(ConnectorHealthSnapshotDO::getCheckedAt);
        if (checkType != null) {
            wrapper.eq(ConnectorHealthSnapshotDO::getCheckType, checkType.name());
        }
        if (healthStatus != null) {
            wrapper.eq(ConnectorHealthSnapshotDO::getHealthStatus, healthStatus.name());
        }
        if (checkedFrom != null) {
            wrapper.ge(ConnectorHealthSnapshotDO::getCheckedAt, toLocalDateTime(checkedFrom));
        }
        if (checkedTo != null) {
            wrapper.le(ConnectorHealthSnapshotDO::getCheckedAt, toLocalDateTime(checkedTo));
        }
        Page<ConnectorHealthSnapshotDO> page = connectorHealthSnapshotMapper.selectPage(Page.of(1, limit), wrapper);
        return page.getRecords().stream().map(this::toHealthSnapshot).toList();
    }

    private ConnectorDefinition toDomain(ConnectorDefinitionDO definitionDO) {
        List<ConnectorParameter> parameters = connectorParameterMapper.selectList(
                new LambdaQueryWrapper<ConnectorParameterDO>()
                        .eq(ConnectorParameterDO::getConnectorId, definitionDO.getId())
                        .orderByAsc(ConnectorParameterDO::getParamKey)
        ).stream().map(this::toParameter).toList();
        return new ConnectorDefinition(
                definitionDO.getId(),
                definitionDO.getTenantId(),
                definitionDO.getCode(),
                definitionDO.getName(),
                ConnectorType.valueOf(definitionDO.getConnectorType()),
                definitionDO.getVendor(),
                definitionDO.getProtocol(),
                com.hjo2oa.data.connector.domain.ConnectorAuthMode.valueOf(definitionDO.getAuthMode()),
                connectorJsonCodec.readTimeoutConfig(definitionDO.getTimeoutConfig()),
                ConnectorStatus.valueOf(definitionDO.getStatus()),
                definitionDO.getChangeSequence() == null ? 0L : definitionDO.getChangeSequence(),
                parameters,
                toInstant(definitionDO.getCreatedAt()),
                toInstant(definitionDO.getUpdatedAt())
        );
    }

    private ConnectorDefinitionDO toDefinitionDO(ConnectorDefinition connectorDefinition) {
        ConnectorDefinitionDO definitionDO = new ConnectorDefinitionDO();
        definitionDO.setId(connectorDefinition.connectorId());
        definitionDO.setTenantId(connectorDefinition.tenantId());
        definitionDO.setCode(connectorDefinition.code());
        definitionDO.setName(connectorDefinition.name());
        definitionDO.setConnectorType(connectorDefinition.connectorType().name());
        definitionDO.setVendor(connectorDefinition.vendor());
        definitionDO.setProtocol(connectorDefinition.protocol());
        definitionDO.setAuthMode(connectorDefinition.authMode().name());
        definitionDO.setTimeoutConfig(connectorJsonCodec.writeTimeoutConfig(connectorDefinition.timeoutConfig()));
        definitionDO.setStatus(connectorDefinition.status().name());
        definitionDO.setChangeSequence(connectorDefinition.changeSequence());
        definitionDO.setCreatedAt(toLocalDateTime(connectorDefinition.createdAt()));
        definitionDO.setUpdatedAt(toLocalDateTime(connectorDefinition.updatedAt()));
        definitionDO.setDeleted(0);
        return definitionDO;
    }

    private ConnectorParameterDO toParameterDO(ConnectorParameter connectorParameter, Instant updatedAt) {
        ConnectorParameterDO parameterDO = new ConnectorParameterDO();
        parameterDO.setId(connectorParameter.parameterId());
        parameterDO.setConnectorId(connectorParameter.connectorId());
        parameterDO.setParamKey(connectorParameter.paramKey());
        parameterDO.setParamValueRef(connectorParameter.paramValueRef());
        parameterDO.setSensitive(connectorParameter.sensitive());
        parameterDO.setCreatedAt(toLocalDateTime(updatedAt));
        parameterDO.setUpdatedAt(toLocalDateTime(updatedAt));
        return parameterDO;
    }

    private ConnectorParameter toParameter(ConnectorParameterDO parameterDO) {
        return new ConnectorParameter(
                parameterDO.getId(),
                parameterDO.getConnectorId(),
                parameterDO.getParamKey(),
                parameterDO.getParamValueRef(),
                Boolean.TRUE.equals(parameterDO.getSensitive())
        );
    }

    private ConnectorHealthSnapshotDO toHealthSnapshotDO(ConnectorHealthSnapshot healthSnapshot) {
        ConnectorHealthSnapshotDO healthSnapshotDO = new ConnectorHealthSnapshotDO();
        healthSnapshotDO.setId(healthSnapshot.snapshotId());
        healthSnapshotDO.setConnectorId(healthSnapshot.connectorId());
        healthSnapshotDO.setCheckType(healthSnapshot.checkType().name());
        healthSnapshotDO.setHealthStatus(healthSnapshot.healthStatus().name());
        healthSnapshotDO.setLatencyMs(healthSnapshot.latencyMs());
        healthSnapshotDO.setErrorCode(healthSnapshot.errorCode());
        healthSnapshotDO.setErrorSummary(healthSnapshot.errorSummary());
        healthSnapshotDO.setOperatorId(healthSnapshot.operatorId());
        healthSnapshotDO.setTargetEnvironment(healthSnapshot.targetEnvironment());
        healthSnapshotDO.setConfirmedBy(healthSnapshot.confirmedBy());
        healthSnapshotDO.setConfirmationNote(healthSnapshot.confirmationNote());
        healthSnapshotDO.setConfirmedAt(toLocalDateTimeNullable(healthSnapshot.confirmedAt()));
        healthSnapshotDO.setChangeSequence(healthSnapshot.changeSequence());
        healthSnapshotDO.setCheckedAt(toLocalDateTime(healthSnapshot.checkedAt()));
        return healthSnapshotDO;
    }

    private ConnectorHealthSnapshot toHealthSnapshot(ConnectorHealthSnapshotDO healthSnapshotDO) {
        return new ConnectorHealthSnapshot(
                healthSnapshotDO.getId(),
                healthSnapshotDO.getConnectorId(),
                ConnectorCheckType.valueOf(healthSnapshotDO.getCheckType()),
                ConnectorHealthStatus.valueOf(healthSnapshotDO.getHealthStatus()),
                healthSnapshotDO.getLatencyMs() == null ? 0L : healthSnapshotDO.getLatencyMs(),
                healthSnapshotDO.getErrorCode(),
                healthSnapshotDO.getErrorSummary(),
                healthSnapshotDO.getOperatorId(),
                healthSnapshotDO.getTargetEnvironment(),
                healthSnapshotDO.getConfirmedBy(),
                healthSnapshotDO.getConfirmationNote(),
                toInstantNullable(healthSnapshotDO.getConfirmedAt()),
                healthSnapshotDO.getChangeSequence() == null ? 0L : healthSnapshotDO.getChangeSequence(),
                toInstant(healthSnapshotDO.getCheckedAt())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTimeNullable(Instant instant) {
        return instant == null ? null : toLocalDateTime(instant);
    }

    private Instant toInstantNullable(LocalDateTime localDateTime) {
        return localDateTime == null ? null : toInstant(localDateTime);
    }
}
