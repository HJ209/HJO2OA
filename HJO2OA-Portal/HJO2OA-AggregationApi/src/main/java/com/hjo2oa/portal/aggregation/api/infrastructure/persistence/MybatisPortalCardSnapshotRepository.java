package com.hjo2oa.portal.aggregation.api.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshotRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalCardSnapshotRepository implements PortalCardSnapshotRepository {

    private final PortalCardSnapshotMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPortalCardSnapshotRepository(PortalCardSnapshotMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PortalCardSnapshot<?>> findByKey(PortalAggregationSnapshotKey snapshotKey) {
        return Optional.ofNullable(mapper.selectById(snapshotKey.asCacheKey())).map(this::toDomain);
    }

    @Override
    public void save(PortalCardSnapshot<?> snapshot) {
        PortalCardSnapshotEntity existing = mapper.selectById(snapshot.snapshotKey().asCacheKey());
        PortalCardSnapshotEntity entity = toEntity(snapshot, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    @Override
    public int markStale(PortalSnapshotScope scope, Set<PortalCardType> cardTypes, String reason, Instant staleAt) {
        List<PortalCardSnapshotEntity> matches = mapper.selectList(new QueryWrapper<PortalCardSnapshotEntity>()
                        .in(cardTypes != null && !cardTypes.isEmpty(), "card_type",
                                cardTypes == null ? List.of() : cardTypes.stream().map(Enum::name).toList()))
                .stream()
                .filter(entity -> scope.matches(keyOf(entity)))
                .toList();
        for (PortalCardSnapshotEntity entity : matches) {
            entity.setState(PortalCardState.STALE.name());
            entity.setMessage(reason);
            entity.setRefreshedAt(staleAt);
            mapper.updateById(entity);
        }
        return matches.size();
    }

    @Override
    public List<PortalCardSnapshot<?>> findAll() {
        return mapper.selectList(new QueryWrapper<PortalCardSnapshotEntity>().orderByDesc("refreshed_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private PortalCardSnapshot<?> toDomain(PortalCardSnapshotEntity entity) {
        PortalCardType cardType = PortalCardType.valueOf(entity.getCardType());
        return new PortalCardSnapshot<>(
                keyOf(entity),
                cardType,
                PortalCardState.valueOf(entity.getState()),
                readData(entity.getDataJson(), cardType),
                entity.getMessage(),
                entity.getRefreshedAt()
        );
    }

    private PortalAggregationSnapshotKey keyOf(PortalCardSnapshotEntity entity) {
        return new PortalAggregationSnapshotKey(
                entity.getTenantId(),
                entity.getPersonId(),
                entity.getAssignmentId(),
                entity.getPositionId(),
                PortalSceneType.valueOf(entity.getSceneType()),
                PortalCardType.valueOf(entity.getCardType())
        );
    }

    private PortalCardSnapshotEntity toEntity(PortalCardSnapshot<?> snapshot, PortalCardSnapshotEntity existing) {
        PortalAggregationSnapshotKey key = snapshot.snapshotKey();
        PortalCardSnapshotEntity entity = existing == null ? new PortalCardSnapshotEntity() : existing;
        entity.setSnapshotId(key.asCacheKey());
        entity.setTenantId(key.tenantId());
        entity.setPersonId(key.personId());
        entity.setAssignmentId(key.assignmentId());
        entity.setPositionId(key.positionId());
        entity.setSceneType(key.sceneType().name());
        entity.setCardType(snapshot.cardType().name());
        entity.setState(snapshot.state().name());
        entity.setDataJson(writeJson(snapshot.data()));
        entity.setMessage(snapshot.message());
        entity.setRefreshedAt(snapshot.refreshedAt());
        return entity;
    }

    private Object readData(String json, PortalCardType cardType) {
        try {
            return objectMapper.readerFor(dataTypeOf(cardType))
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid portal snapshot data JSON", ex);
        }
    }

    private Class<?> dataTypeOf(PortalCardType cardType) {
        return switch (cardType) {
            case IDENTITY -> PortalIdentityCard.class;
            case TODO -> PortalTodoCard.class;
            case MESSAGE -> PortalMessageCard.class;
            case CONTENT -> PortalContentCard.class;
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write portal snapshot data JSON", ex);
        }
    }
}
