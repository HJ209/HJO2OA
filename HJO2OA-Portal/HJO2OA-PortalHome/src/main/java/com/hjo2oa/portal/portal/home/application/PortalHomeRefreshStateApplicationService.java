package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshState;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateScope;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStateRepository;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStatus;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalHomeRefreshStateApplicationService {

    private final PortalHomeRefreshStateRepository refreshStateRepository;
    private final PersonalizationIdentityContextProvider identityContextProvider;
    private final Clock clock;

    @Autowired
    public PortalHomeRefreshStateApplicationService(
            PortalHomeRefreshStateRepository refreshStateRepository,
            PersonalizationIdentityContextProvider identityContextProvider
    ) {
        this(refreshStateRepository, identityContextProvider, Clock.systemUTC());
    }

    public PortalHomeRefreshStateApplicationService(
            PortalHomeRefreshStateRepository refreshStateRepository,
            PersonalizationIdentityContextProvider identityContextProvider,
            Clock clock
    ) {
        this.refreshStateRepository = Objects.requireNonNull(
                refreshStateRepository,
                "refreshStateRepository must not be null"
        );
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalHomeRefreshState currentState(PortalHomeSceneType sceneType) {
        PersonalizationIdentityContext identityContext = identityContextProvider.currentContext();
        return currentState(identityContext, sceneType);
    }

    PortalHomeRefreshState currentState(
            PersonalizationIdentityContext identityContext,
            PortalHomeSceneType sceneType
    ) {
        Objects.requireNonNull(identityContext, "identityContext must not be null");
        PortalHomeSceneType resolvedSceneType = sceneType == null ? PortalHomeSceneType.HOME : sceneType;
        return refreshStateRepository.findCurrent(
                        identityContext.tenantId(),
                        identityContext.personId(),
                        identityContext.assignmentId(),
                        resolvedSceneType
                )
                .orElseGet(() -> PortalHomeRefreshState.idle(resolvedSceneType, clock.instant()));
    }

    public PortalHomeRefreshState markReloadRequired(
            PortalHomeSceneType sceneType,
            String triggerEvent,
            Instant updatedAt
    ) {
        PersonalizationIdentityContext identityContext = identityContextProvider.currentContext();
        return markReloadRequiredForIdentity(
                identityContext.tenantId(),
                identityContext.personId(),
                identityContext.assignmentId(),
                sceneType,
                triggerEvent,
                updatedAt
        );
    }

    public PortalHomeRefreshState markReloadRequiredForIdentity(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType,
            String triggerEvent,
            Instant updatedAt
    ) {
        return save(
                PortalHomeRefreshStateScope.ofIdentity(tenantId, personId, assignmentId, requireSceneType(sceneType)),
                new PortalHomeRefreshState(
                        requireSceneType(sceneType),
                        PortalHomeRefreshStatus.RELOAD_REQUIRED,
                        triggerEvent,
                        null,
                        "Page data should be reloaded",
                        requireUpdatedAt(updatedAt)
                )
        );
    }

    public PortalHomeRefreshState markReloadRequiredForPerson(
            String tenantId,
            String personId,
            PortalHomeSceneType sceneType,
            String triggerEvent,
            Instant updatedAt
    ) {
        return save(
                PortalHomeRefreshStateScope.ofPerson(tenantId, personId, requireSceneType(sceneType)),
                new PortalHomeRefreshState(
                        requireSceneType(sceneType),
                        PortalHomeRefreshStatus.RELOAD_REQUIRED,
                        triggerEvent,
                        null,
                        "Page data should be reloaded",
                        requireUpdatedAt(updatedAt)
                )
        );
    }

    public PortalHomeRefreshState markReloadRequiredForTenant(
            String tenantId,
            PortalHomeSceneType sceneType,
            String triggerEvent,
            Instant updatedAt
    ) {
        return save(
                PortalHomeRefreshStateScope.ofTenant(tenantId, requireSceneType(sceneType)),
                new PortalHomeRefreshState(
                        requireSceneType(sceneType),
                        PortalHomeRefreshStatus.RELOAD_REQUIRED,
                        triggerEvent,
                        null,
                        "Page data should be reloaded",
                        requireUpdatedAt(updatedAt)
                )
        );
    }

    public void markReloadRequiredAllScenesForPerson(
            String tenantId,
            String personId,
            String triggerEvent,
            Instant updatedAt
    ) {
        Instant resolvedUpdatedAt = requireUpdatedAt(updatedAt);
        for (PortalHomeSceneType sceneType : PortalHomeSceneType.values()) {
            markReloadRequiredForPerson(tenantId, personId, sceneType, triggerEvent, resolvedUpdatedAt);
        }
    }

    public void markReloadRequiredAllScenesForTenant(
            String tenantId,
            String triggerEvent,
            Instant updatedAt
    ) {
        Instant resolvedUpdatedAt = requireUpdatedAt(updatedAt);
        for (PortalHomeSceneType sceneType : PortalHomeSceneType.values()) {
            markReloadRequiredForTenant(tenantId, sceneType, triggerEvent, resolvedUpdatedAt);
        }
    }

    public void markReloadRequiredAllScenes(String triggerEvent, Instant updatedAt) {
        PersonalizationIdentityContext identityContext = identityContextProvider.currentContext();
        markReloadRequiredAllScenesForIdentity(
                identityContext.tenantId(),
                identityContext.personId(),
                identityContext.assignmentId(),
                triggerEvent,
                updatedAt
        );
    }

    public void markReloadRequiredAllScenesForIdentity(
            String tenantId,
            String personId,
            String assignmentId,
            String triggerEvent,
            Instant updatedAt
    ) {
        Instant resolvedUpdatedAt = requireUpdatedAt(updatedAt);
        for (PortalHomeSceneType sceneType : PortalHomeSceneType.values()) {
            markReloadRequiredForIdentity(
                    tenantId,
                    personId,
                    assignmentId,
                    sceneType,
                    triggerEvent,
                    resolvedUpdatedAt
            );
        }
    }

    public PortalHomeRefreshState markCardRefreshed(
            PortalHomeSceneType sceneType,
            PortalCardType cardType,
            String triggerEvent,
            Instant updatedAt
    ) {
        PersonalizationIdentityContext identityContext = identityContextProvider.currentContext();
        return markCardRefreshed(
                identityContext.tenantId(),
                identityContext.personId(),
                identityContext.assignmentId(),
                sceneType,
                cardType,
                triggerEvent,
                updatedAt
        );
    }

    public PortalHomeRefreshState markCardRefreshed(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType,
            PortalCardType cardType,
            String triggerEvent,
            Instant updatedAt
    ) {
        return save(
                PortalHomeRefreshStateScope.ofIdentity(tenantId, personId, assignmentId, requireSceneType(sceneType)),
                new PortalHomeRefreshState(
                        requireSceneType(sceneType),
                        PortalHomeRefreshStatus.CARD_REFRESHED,
                        triggerEvent,
                        cardType == null ? null : cardType.name(),
                        "Card snapshot refreshed",
                        requireUpdatedAt(updatedAt)
                )
        );
    }

    public PortalHomeRefreshState markCardFailed(
            PortalHomeSceneType sceneType,
            PortalCardType cardType,
            String triggerEvent,
            String message,
            Instant updatedAt
    ) {
        PersonalizationIdentityContext identityContext = identityContextProvider.currentContext();
        return markCardFailed(
                identityContext.tenantId(),
                identityContext.personId(),
                identityContext.assignmentId(),
                sceneType,
                cardType,
                triggerEvent,
                message,
                updatedAt
        );
    }

    public PortalHomeRefreshState markCardFailed(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType,
            PortalCardType cardType,
            String triggerEvent,
            String message,
            Instant updatedAt
    ) {
        return save(
                PortalHomeRefreshStateScope.ofIdentity(tenantId, personId, assignmentId, requireSceneType(sceneType)),
                new PortalHomeRefreshState(
                        requireSceneType(sceneType),
                        PortalHomeRefreshStatus.CARD_FAILED,
                        triggerEvent,
                        cardType == null ? null : cardType.name(),
                        message,
                        requireUpdatedAt(updatedAt)
                )
        );
    }

    private PortalHomeSceneType requireSceneType(PortalHomeSceneType sceneType) {
        return Objects.requireNonNull(sceneType, "sceneType must not be null");
    }

    private PortalHomeRefreshState save(
            PortalHomeRefreshStateScope scope,
            PortalHomeRefreshState refreshState
    ) {
        return refreshStateRepository.save(
                Objects.requireNonNull(scope, "scope must not be null"),
                Objects.requireNonNull(refreshState, "refreshState must not be null")
        );
    }

    private Instant requireUpdatedAt(Instant updatedAt) {
        return Objects.requireNonNullElseGet(updatedAt, clock::instant);
    }
}
