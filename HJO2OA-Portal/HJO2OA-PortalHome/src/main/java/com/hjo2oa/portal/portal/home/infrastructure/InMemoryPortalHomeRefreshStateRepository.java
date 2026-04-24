package com.hjo2oa.portal.portal.home.infrastructure;

import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshState;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateRepository;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateScope;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalHomeRefreshStateRepository implements PortalHomeRefreshStateRepository {

    private final Map<PortalHomeRefreshStateScope, PortalHomeRefreshState> refreshStates = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalHomeRefreshState> findCurrent(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType
    ) {
        PortalHomeSceneType resolvedSceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        PortalHomeRefreshState current = null;
        if (assignmentId != null && personId != null) {
            current = selectCurrent(
                    current,
                    refreshStates.get(
                    PortalHomeRefreshStateScope.ofIdentity(tenantId, personId, assignmentId, resolvedSceneType)
                    )
            );
        }
        if (personId != null) {
            current = selectCurrent(
                    current,
                    refreshStates.get(
                    PortalHomeRefreshStateScope.ofPerson(tenantId, personId, resolvedSceneType)
                    )
            );
        }
        current = selectCurrent(
                current,
                refreshStates.get(PortalHomeRefreshStateScope.ofTenant(tenantId, resolvedSceneType))
        );
        return Optional.ofNullable(current);
    }

    @Override
    public PortalHomeRefreshState save(
            PortalHomeRefreshStateScope scope,
            PortalHomeRefreshState refreshState
    ) {
        refreshStates.put(
                Objects.requireNonNull(scope, "scope must not be null"),
                Objects.requireNonNull(refreshState, "refreshState must not be null")
        );
        return refreshState;
    }

    private PortalHomeRefreshState selectCurrent(
            PortalHomeRefreshState current,
            PortalHomeRefreshState candidate
    ) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.updatedAt().isAfter(current.updatedAt())) {
            return candidate;
        }
        return current;
    }
}
