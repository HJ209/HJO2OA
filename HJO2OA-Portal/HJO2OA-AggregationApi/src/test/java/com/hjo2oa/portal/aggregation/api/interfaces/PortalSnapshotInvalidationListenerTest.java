package com.hjo2oa.portal.aggregation.api.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;

class PortalSnapshotInvalidationListenerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T11:30:00Z");

    @Test
    void shouldInvalidateAllCurrentIdentitySnapshotsWhenIdentitySwitches() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = new PortalSnapshotInvalidationListener(
                new PortalSnapshotInvalidationApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME.plusSeconds(60), ZoneOffset.UTC)
                )
        );

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

        assertThat(repository.findAll())
                .extracting(PortalCardSnapshot::state)
                .containsOnly(PortalCardState.STALE);
    }

    @Test
    void shouldInvalidateOnlyMessageSnapshotsWhenNotificationIsSent() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = new PortalSnapshotInvalidationListener(
                new PortalSnapshotInvalidationApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME.plusSeconds(90), ZoneOffset.UTC)
                )
        );

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
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyMessageSnapshotsWhenNotificationIsRead() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = new PortalSnapshotInvalidationListener(
                new PortalSnapshotInvalidationApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME.plusSeconds(120), ZoneOffset.UTC)
                )
        );

        listener.onNotificationRead(new MsgNotificationReadEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "notification-1",
                "assignment-1",
                FIXED_TIME
        ));

        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.READY);
    }

    @Test
    void shouldInvalidateOnlyTodoSnapshotsWhenTodoItemBecomesOverdue() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        seedSnapshots(repository);
        PortalSnapshotInvalidationListener listener = new PortalSnapshotInvalidationListener(
                new PortalSnapshotInvalidationApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME.plusSeconds(180), ZoneOffset.UTC)
                )
        );

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
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.STALE);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.READY);
    }

    private void seedSnapshots(InMemoryPortalCardSnapshotRepository repository) {
        PortalIdentityCard identity = new PortalIdentityCard(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "position-1",
                "organization-1",
                "department-1",
                "Chief Clerk",
                "Head Office",
                "General Office",
                "PRIMARY",
                FIXED_TIME
        );
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.TODO),
                PortalCardType.TODO,
                PortalTodoCard.empty(),
                FIXED_TIME
        ));
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.MESSAGE),
                PortalCardType.MESSAGE,
                PortalMessageCard.empty(),
                FIXED_TIME
        ));
    }
}
