package com.hjo2oa.portal.portal.home.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotFailedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotRefreshedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.home.application.PortalHomeRefreshStateApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRefreshStatus;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.home.infrastructure.InMemoryPortalHomeRefreshStateRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalHomeRefreshStateEventListenerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T14:00:00Z");

    @Test
    void shouldRecordCardRefreshedStateWhenSnapshotRefreshEventArrives() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onSnapshotRefreshed(new PortalSnapshotRefreshedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "person-1",
                "assignment-1",
                "portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:TODO",
                PortalSceneType.HOME,
                PortalCardType.TODO,
                FIXED_TIME.plusSeconds(30)
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.CARD_REFRESHED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).cardType())
                .isEqualTo("TODO");
    }

    @Test
    void shouldRecordCardFailedStateWhenSnapshotFailureEventArrives() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onSnapshotFailed(new PortalSnapshotFailedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "person-1",
                "assignment-1",
                "portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:MESSAGE",
                PortalSceneType.HOME,
                PortalCardType.MESSAGE,
                "Message card is temporarily unavailable"
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.CARD_FAILED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).message())
                .isEqualTo("Message card is temporarily unavailable");
    }

    @Test
    void shouldMarkAllScenesForReloadWhenIdentitySwitches() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        refreshStateApplicationService.markCardFailed(
                PortalHomeSceneType.HOME,
                PortalCardType.MESSAGE,
                "portal.snapshot.failed",
                "Message card is temporarily unavailable",
                FIXED_TIME.minusSeconds(30)
        );
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onIdentitySwitched(new IdentitySwitchedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                "position-1",
                "position-2",
                IdentityAssignmentType.PRIMARY,
                IdentityAssignmentType.SECONDARY,
                "manual-switch"
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).triggerEvent())
                .isEqualTo(IdentitySwitchedEvent.EVENT_TYPE);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.MOBILE_WORKBENCH).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    @Test
    void shouldMarkAllScenesForReloadWhenIdentityContextIsInvalidated() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onIdentityContextInvalidated(new IdentityContextInvalidatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                IdentityContextInvalidationReason.PRIMARY_CHANGED,
                false,
                2L,
                "org.assignment.changed"
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).triggerEvent())
                .isEqualTo(IdentityContextInvalidatedEvent.EVENT_TYPE);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.MOBILE_WORKBENCH).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    @Test
    void shouldMarkAllScenesForReloadWhenPublicationOfflinedHasNoScene() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onPublicationOfflined(new PortalPublicationOfflinedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "publication-1",
                "template-1",
                null
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.MOBILE_WORKBENCH).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    @Test
    void shouldMarkTargetSceneForReloadWhenPersonalizationChanges() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onPersonalizationSaved(new PortalPersonalizationSavedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "profile-1",
                "person-1",
                PersonalizationSceneType.OFFICE_CENTER
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.IDLE);
    }

    @Test
    void shouldMarkTargetSceneForReloadWhenWidgetIsUpdated() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onWidgetUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                java.util.List.of("displayName")
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.IDLE);
    }

    @Test
    void shouldMarkAllScenesForReloadWhenWidgetUpdateIsTenantWide() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onWidgetUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-3",
                "identity-card",
                WidgetCardType.IDENTITY,
                null,
                java.util.List.of("sceneType")
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.MOBILE_WORKBENCH).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    @Test
    void shouldMarkAllScenesForReloadWhenWidgetIsDisabledWithoutScene() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService = refreshStateApplicationService();
        PortalHomeRefreshStateEventListener listener =
                new PortalHomeRefreshStateEventListener(refreshStateApplicationService);

        listener.onWidgetDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
        ));

        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.HOME).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.OFFICE_CENTER).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
        assertThat(refreshStateApplicationService.currentState(PortalHomeSceneType.MOBILE_WORKBENCH).status())
                .isEqualTo(PortalHomeRefreshStatus.RELOAD_REQUIRED);
    }

    private PortalHomeRefreshStateApplicationService refreshStateApplicationService() {
        return new PortalHomeRefreshStateApplicationService(
                new InMemoryPortalHomeRefreshStateRepository(),
                () -> new PersonalizationIdentityContext("tenant-1", "person-1", "assignment-1", "position-1"),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
