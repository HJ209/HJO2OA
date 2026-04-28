package com.hjo2oa.portal.portal.home.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshState;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateRepository;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateScope;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStatus;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPortalHomeRefreshStateRepository implements PortalHomeRefreshStateRepository {

    private final PortalHomeRefreshStateMapper mapper;

    public MybatisPortalHomeRefreshStateRepository(PortalHomeRefreshStateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PortalHomeRefreshState> findCurrent(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType
    ) {
        PortalHomeSceneType resolvedSceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        return mapper.selectList(new QueryWrapper<PortalHomeRefreshStateEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("scene_type", resolvedSceneType.name()))
                .stream()
                .filter(entity -> matchesScope(entity, personId, assignmentId))
                .max(Comparator.comparing(PortalHomeRefreshStateEntity::getUpdatedAt))
                .map(this::toDomain);
    }

    @Override
    public PortalHomeRefreshState save(PortalHomeRefreshStateScope scope, PortalHomeRefreshState refreshState) {
        PortalHomeRefreshStateEntity existing = mapper.selectOne(scopeQuery(scope));
        PortalHomeRefreshStateEntity entity = existing == null ? new PortalHomeRefreshStateEntity() : existing;
        entity.setId(scopeId(scope));
        entity.setTenantId(scope.tenantId());
        entity.setPersonId(scope.personId());
        entity.setAssignmentId(scope.assignmentId());
        entity.setSceneType(scope.sceneType().name());
        entity.setStatus(refreshState.status().name());
        entity.setTriggerEvent(refreshState.triggerEvent());
        entity.setCardType(refreshState.cardType());
        entity.setMessage(refreshState.message());
        entity.setUpdatedAt(refreshState.updatedAt());
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return refreshState;
    }

    private QueryWrapper<PortalHomeRefreshStateEntity> scopeQuery(PortalHomeRefreshStateScope scope) {
        QueryWrapper<PortalHomeRefreshStateEntity> wrapper = new QueryWrapper<PortalHomeRefreshStateEntity>()
                .eq("tenant_id", scope.tenantId())
                .eq("scene_type", scope.sceneType().name());
        if (scope.personId() == null) {
            wrapper.isNull("person_id");
        } else {
            wrapper.eq("person_id", scope.personId());
        }
        if (scope.assignmentId() == null) {
            wrapper.isNull("assignment_id");
        } else {
            wrapper.eq("assignment_id", scope.assignmentId());
        }
        return wrapper;
    }

    private boolean matchesScope(PortalHomeRefreshStateEntity entity, String personId, String assignmentId) {
        if (entity.getAssignmentId() != null) {
            return entity.getAssignmentId().equals(assignmentId) && entity.getPersonId().equals(personId);
        }
        return entity.getPersonId() == null || entity.getPersonId().equals(personId);
    }

    private PortalHomeRefreshState toDomain(PortalHomeRefreshStateEntity entity) {
        return new PortalHomeRefreshState(
                PortalHomeSceneType.valueOf(entity.getSceneType()),
                PortalHomeRefreshStatus.valueOf(entity.getStatus()),
                entity.getTriggerEvent(),
                entity.getCardType(),
                entity.getMessage(),
                entity.getUpdatedAt()
        );
    }

    private String scopeId(PortalHomeRefreshStateScope scope) {
        return String.join("::",
                scope.tenantId(),
                value(scope.personId()),
                value(scope.assignmentId()),
                scope.sceneType().name());
    }

    private String value(String value) {
        return value == null ? "-" : value;
    }
}
