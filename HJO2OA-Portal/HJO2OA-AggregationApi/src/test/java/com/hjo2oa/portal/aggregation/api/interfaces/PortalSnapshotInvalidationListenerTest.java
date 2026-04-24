package com.hjo2oa.portal.aggregation.api.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.aggregation.api.application.PortalSnapshotInvalidationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.infrastructure.InMemoryPortalCardSnapshotRepository;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalSnapshotInvalidationListenerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T11:30:00Z");

    @Test
    void shouldInvalidateOldAndNewIdentitySnapshotsWhenIdentitySwitches() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 60);

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
                "switch"
        ));

        assertSnapshotsInState(repository, "person-1", "assignment-1", PortalCardState.STALE);
        assertSnapshotsInState(repository, "person-1", "assignment-2", PortalCardState.STALE);
        assertSnapshotsInState(repository, "person-1", "assignment-3", PortalCardState.READY);
        assertSnapshotsInState(repository, "person-2", "assignment-9", PortalCardState.READY);
    }

    @Test
    void shouldInvalidateInvalidatedAndFallbackIdentitySnapshotsWhenContextIsRefreshed() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 75);

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

        assertSnapshotsInState(repository, "person-1", "assignment-1", PortalCardState.STALE);
        assertSnapshotsInState(repository, "person-1", "assignment-2", PortalCardState.STALE);
        assertSnapshotsInState(repository, "person-1", "assignment-3", PortalCardState.READY);
        assertSnapshotsInState(repository, "person-2", "assignment-9", PortalCardState.READY);
    }

    @Test
    void shouldInvalidateAllPersonSnapshotsWhenIdentityRefreshRequiresRelogin() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 90);

        listener.onIdentityContextInvalidated(new IdentityContextInvalidatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                null,
                IdentityContextInvalidationReason.ACCOUNT_LOCKED,
                true,
                3L,
                "org.account.locked"
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().personId().equals("person-1"))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertSnapshotsInState(repository, "person-2", "assignment-9", PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyMessageSnapshotsWhenNotificationIsSent() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 120);

        listener.onNotificationSent(new MsgNotificationSentEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "notification-1",
                "assignment-1",
                "INBOX",
                "TODO_CREATED",
                "todo-center"
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE
                        && snapshot.snapshotKey().assignmentId().equals("assignment-1"))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE
                        && !snapshot.snapshotKey().assignmentId().equals("assignment-1"))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() != PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyMessageSnapshotsWhenNotificationIsRead() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 150);

        listener.onNotificationRead(new MsgNotificationReadEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "notification-1",
                "assignment-1",
                FIXED_TIME
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE
                        && snapshot.snapshotKey().assignmentId().equals("assignment-1"))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() != PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyTodoSnapshotsWhenTodoItemBecomesOverdue() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 180);

        listener.onTodoItemOverdue(new TodoItemOverdueEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                FIXED_TIME.minusSeconds(600),
                Duration.ofMinutes(10)
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.TODO
                        && snapshot.snapshotKey().assignmentId().equals("assignment-1"))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() != PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyMatchingSceneSnapshotsWhenPublicationIsActivated() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 210);

        listener.onPublicationActivated(new PortalPublicationActivatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "publication-1",
                "template-1",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.HOME)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyMatchingPersonSceneSnapshotsWhenPersonalizationIsSaved() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 240);

        listener.onPersonalizationSaved(new PortalPersonalizationSavedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "profile-1",
                "person-1",
                PersonalizationSceneType.HOME
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().personId().equals("person-1")
                        && snapshot.snapshotKey().sceneType() == PortalSceneType.HOME)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
        assertSnapshotsInState(repository, "person-2", "assignment-9", PortalCardState.READY);
    }

    @Test
    void shouldInvalidateAllTenantScenesWhenPublicationIsOfflinedWithoutSceneType() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 270);

        listener.onPublicationOfflined(new PortalPublicationOfflinedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "publication-2",
                "template-2",
                null
        ));

        assertThat(repository.findAll())
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
    }

    @Test
    void shouldInvalidateOnlyMatchingSceneCardSnapshotsWhenWidgetIsUpdated() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 300);

        listener.onWidgetUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                java.util.List.of("displayName", "allowHide")
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER
                        && snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.HOME
                        && snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() != PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidatePreviousAndCurrentWidgetTargetsWhenWidgetIsMoved() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 315);

        listener.onWidgetUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-1",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.OFFICE_CENTER,
                WidgetCardType.TODO,
                WidgetSceneType.HOME,
                java.util.List.of("cardType", "sceneType")
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.HOME
                        && snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER
                        && snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> !(snapshot.snapshotKey().sceneType() == PortalSceneType.HOME
                        && snapshot.cardType() == PortalCardType.TODO)
                        && !(snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER
                        && snapshot.cardType() == PortalCardType.MESSAGE))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateAllTenantCardSnapshotsWhenWidgetIsDisabledWithoutSceneType() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = listener(repository, 330);

        listener.onWidgetDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() != PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.READY);
    }

    private PortalSnapshotInvalidationListener listener(
            InMemoryPortalCardSnapshotRepository repository,
            long offsetSeconds
    ) {
        return new PortalSnapshotInvalidationListener(
                new PortalSnapshotInvalidationApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME.plusSeconds(offsetSeconds), ZoneOffset.UTC)
                )
        );
    }

    private void seedSnapshots(InMemoryPortalCardSnapshotRepository repository) {
        saveSnapshots(repository, identity("person-1", "assignment-1", "position-1"), PortalSceneType.HOME);
        saveSnapshots(repository, identity("person-1", "assignment-1", "position-1"), PortalSceneType.OFFICE_CENTER);
        saveSnapshots(repository, identity("person-1", "assignment-2", "position-2"), PortalSceneType.HOME);
        saveSnapshots(repository, identity("person-1", "assignment-3", "position-3"), PortalSceneType.HOME);
        saveSnapshots(repository, identity("person-2", "assignment-9", "position-9"), PortalSceneType.HOME);
    }

    private void saveSnapshots(
            InMemoryPortalCardSnapshotRepository repository,
            PortalIdentityCard identity,
            PortalSceneType sceneType
    ) {
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, sceneType, PortalCardType.IDENTITY),
                PortalCardType.IDENTITY,
                identity,
                FIXED_TIME
        ));
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, sceneType, PortalCardType.TODO),
                PortalCardType.TODO,
                PortalTodoCard.empty(),
                FIXED_TIME
        ));
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, sceneType, PortalCardType.MESSAGE),
                PortalCardType.MESSAGE,
                PortalMessageCard.empty(),
                FIXED_TIME
        ));
    }

    private PortalIdentityCard identity(String personId, String assignmentId, String positionId) {
        return new PortalIdentityCard(
                "tenant-1",
                personId,
                "account-" + personId,
                assignmentId,
                positionId,
                "organization-1",
                "department-" + positionId,
                "Position " + positionId,
                "Head Office",
                "Department " + positionId,
                "PRIMARY",
                FIXED_TIME
        );
    }

    private void assertSnapshotsInState(
            InMemoryPortalCardSnapshotRepository repository,
            String personId,
            String assignmentId,
            PortalCardState expectedState
    ) {
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().personId().equals(personId)
                        && snapshot.snapshotKey().assignmentId().equals(assignmentId))
                .extracting(PortalCardSnapshot::state)
                .containsOnly(expectedState);
    }
}
