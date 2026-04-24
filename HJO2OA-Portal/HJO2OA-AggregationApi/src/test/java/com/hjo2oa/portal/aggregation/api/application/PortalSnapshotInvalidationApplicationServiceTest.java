package com.hjo2oa.portal.aggregation.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.infrastructure.InMemoryPortalCardSnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class PortalSnapshotInvalidationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T11:00:00Z");

    @Test
    void shouldMarkMatchingSnapshotsAsStaleByScopeAndCardType() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        PortalIdentityCard identity = identity();
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

        PortalSnapshotInvalidationApplicationService service = new PortalSnapshotInvalidationApplicationService(
                repository,
                Clock.fixed(FIXED_TIME.plusSeconds(120), ZoneOffset.UTC)
        );

        int markedCount = service.markStale(
                PortalSnapshotScope.ofAssignment("tenant-1", "assignment-1"),
                EnumSet.of(PortalCardType.MESSAGE),
                "msg.notification.sent"
        );

        assertThat(markedCount).isEqualTo(1);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.TODO)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.READY);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.cardType() == PortalCardType.MESSAGE)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.STALE);
    }

    @Test
    void shouldMarkOnlyMatchingSceneSnapshotsAsStale() {
        InMemoryPortalCardSnapshotRepository repository = new InMemoryPortalCardSnapshotRepository();
        PortalIdentityCard identity = identity();
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.TODO),
                PortalCardType.TODO,
                PortalTodoCard.empty(),
                FIXED_TIME
        ));
        repository.save(PortalCardSnapshot.ready(
                PortalAggregationSnapshotKey.of(identity, PortalSceneType.OFFICE_CENTER, PortalCardType.TODO),
                PortalCardType.TODO,
                PortalTodoCard.empty(),
                FIXED_TIME
        ));

        PortalSnapshotInvalidationApplicationService service = new PortalSnapshotInvalidationApplicationService(
                repository,
                Clock.fixed(FIXED_TIME.plusSeconds(180), ZoneOffset.UTC)
        );

        int markedCount = service.markStale(
                PortalSnapshotScope.ofScene("tenant-1", PortalSceneType.OFFICE_CENTER),
                EnumSet.of(PortalCardType.TODO),
                "portal.publication.activated"
        );

        assertThat(markedCount).isEqualTo(1);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.HOME)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.READY);
        assertThat(repository.findAll())
                .filteredOn(snapshot -> snapshot.snapshotKey().sceneType() == PortalSceneType.OFFICE_CENTER)
                .extracting(PortalCardSnapshot::state)
                .containsExactly(PortalCardState.STALE);
    }

    private PortalIdentityCard identity() {
        return new PortalIdentityCard(
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
    }
}
