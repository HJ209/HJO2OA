package com.hjo2oa.portal.portal.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStatus;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.infrastructure.InMemoryPortalHomeRefreshStateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PortalHomeRefreshStateApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T13:00:00Z");

    @Test
    void shouldReturnIdleStateWhenSceneHasNoRefreshSignal() {
        PortalHomeRefreshStateApplicationService service = service(
                new InMemoryPortalHomeRefreshStateRepository(),
                identityContext("tenant-1", "person-1", "assignment-1")
        );

        assertThat(service.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.IDLE);
    }

    @Test
    void shouldStoreCardFailedStateForScene() {
        PortalHomeRefreshStateApplicationService service = service(
                new InMemoryPortalHomeRefreshStateRepository(),
                identityContext("tenant-1", "person-1", "assignment-1")
        );

        service.markCardFailed(
                PortalHomeSceneType.HOME,
                PortalCardType.MESSAGE,
                "portal.snapshot.failed",
                "Message card is temporarily unavailable",
                FIXED_TIME.plusSeconds(30)
        );

        assertThat(service.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.CARD_FAILED);
        assertThat(service.currentState(PortalHomeSceneType.HOME).cardType())
                .isEqualTo("MESSAGE");
    }

    @Test
    void shouldKeepRefreshStatesIsolatedAcrossDifferentIdentities() {
        InMemoryPortalHomeRefreshStateRepository repository = new InMemoryPortalHomeRefreshStateRepository();
        PortalHomeRefreshStateApplicationService firstIdentityService = service(
                repository,
                identityContext("tenant-1", "person-1", "assignment-1")
        );
        PortalHomeRefreshStateApplicationService secondIdentityService = service(
                repository,
                identityContext("tenant-1", "person-2", "assignment-2")
        );

        firstIdentityService.markCardFailed(
                PortalHomeSceneType.HOME,
                PortalCardType.MESSAGE,
                "portal.snapshot.failed",
                "Message card is temporarily unavailable",
                FIXED_TIME.plusSeconds(30)
        );

        assertThat(firstIdentityService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.CARD_FAILED);
        assertThat(secondIdentityService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.IDLE);
    }

    @Test
    void shouldFallBackToTenantWideStateWhenIdentitySpecificStateIsMissing() {
        InMemoryPortalHomeRefreshStateRepository repository = new InMemoryPortalHomeRefreshStateRepository();
        PortalHomeRefreshStateApplicationService service = service(
                repository,
                identityContext("tenant-1", "person-1", "assignment-1")
        );

        service.markReloadRequiredForTenant(
                "tenant-1",
                PortalHomeSceneType.OFFICE_CENTER,
                "portal.widget.updated",
                FIXED_TIME.plusSeconds(45)
        );

        assertThat(service.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    @Test
    void shouldPreferMostRecentBroaderScopeStateOverOlderIdentitySpecificState() {
        InMemoryPortalHomeRefreshStateRepository repository = new InMemoryPortalHomeRefreshStateRepository();
        PortalHomeRefreshStateApplicationService identityService = service(
                repository,
                identityContext("tenant-1", "person-1", "assignment-1")
        );

        identityService.markCardFailed(
                PortalHomeSceneType.HOME,
                PortalCardType.MESSAGE,
                "portal.snapshot.failed",
                "Message card is temporarily unavailable",
                FIXED_TIME.plusSeconds(30)
        );
        identityService.markReloadRequiredForPerson(
                "tenant-1",
                "person-1",
                PortalHomeSceneType.HOME,
                "org.identity.switched",
                FIXED_TIME.plusSeconds(45)
        );

        assertThat(identityService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(identityService.currentState(PortalHomeSceneType.HOME).triggerEvent())
                .isEqualTo("org.identity.switched");
    }

    private PortalHomeRefreshStateApplicationService service(
            InMemoryPortalHomeRefreshStateRepository repository,
            PersonalizationIdentityContext identityContext
    ) {
        return new PortalHomeRefreshStateApplicationService(
                repository,
                () -> identityContext,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PersonalizationIdentityContext identityContext(
            String tenantId,
            String personId,
            String assignmentId
    ) {
        return new PersonalizationIdentityContext(tenantId, personId, assignmentId, "position-" + assignmentId);
    }
}
