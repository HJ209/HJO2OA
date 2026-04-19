package com.hjo2oa.portal.aggregation.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotFailedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotRefreshedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.portal.aggregation.api.infrastructure.InMemoryPortalCardSnapshotRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PortalDashboardAggregationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");

    @Test
    void shouldReturnEmptyCardsAndExpectedSnapshotKeyWhenUpstreamDataIsEmpty() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalDashboardAggregationApplicationService service = new PortalDashboardAggregationApplicationService(
                identityProvider(),
                () -> PortalTodoCard.empty(),
                () -> PortalMessageCard.empty(),
                new InMemoryPortalCardSnapshotRepository(),
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalDashboardView dashboard = service.dashboard(PortalSceneType.HOME, EnumSet.noneOf(PortalCardType.class));

        assertThat(dashboard.identity().state()).isEqualTo(PortalCardState.READY);
        assertThat(dashboard.todo().state()).isEqualTo(PortalCardState.READY);
        assertThat(dashboard.message().state()).isEqualTo(PortalCardState.READY);
        assertThat(dashboard.todo().snapshotKey().asCacheKey())
                .isEqualTo("portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:TODO");
        assertThat(dashboard.message().snapshotKey().asCacheKey())
                .isEqualTo("portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:MESSAGE");
        assertThat(publishedEvents)
                .hasSize(3)
                .allMatch(PortalSnapshotRefreshedEvent.class::isInstance);
    }

    @Test
    void shouldDegradeTodoCardWithoutBreakingDashboardWhenUpstreamFails() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalDashboardAggregationApplicationService service = new PortalDashboardAggregationApplicationService(
                identityProvider(),
                () -> {
                    throw new IllegalStateException("todo unavailable");
                },
                this::messageCard,
                new InMemoryPortalCardSnapshotRepository(),
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalDashboardView dashboard = service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO, PortalCardType.MESSAGE));

        assertThat(dashboard.identity().state()).isEqualTo(PortalCardState.READY);
        assertThat(dashboard.todo().state()).isEqualTo(PortalCardState.FAILED);
        assertThat(dashboard.todo().data().isEmpty()).isTrue();
        assertThat(dashboard.message().state()).isEqualTo(PortalCardState.READY);
        assertThat(dashboard.message().data().unreadCount()).isEqualTo(2);
        assertThat(publishedEvents).hasSize(3);
        assertThat(publishedEvents.get(1)).isInstanceOf(PortalSnapshotFailedEvent.class);
        assertThat(((PortalSnapshotFailedEvent) publishedEvents.get(1)).reason())
                .isEqualTo("Todo card is temporarily unavailable");
    }

    @Test
    void shouldReuseCachedSnapshotsForRepeatedDashboardRequestsButRefreshSingleCardOnDemand() {
        AtomicInteger todoCalls = new AtomicInteger();
        AtomicInteger messageCalls = new AtomicInteger();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        InMemoryPortalCardSnapshotRepository snapshotRepository = new InMemoryPortalCardSnapshotRepository();
        PortalDashboardAggregationApplicationService service = new PortalDashboardAggregationApplicationService(
                identityProvider(),
                () -> {
                    todoCalls.incrementAndGet();
                    return todoCard();
                },
                () -> {
                    messageCalls.incrementAndGet();
                    return messageCard();
                },
                snapshotRepository,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalDashboardView firstDashboard = service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO, PortalCardType.MESSAGE));
        PortalDashboardView secondDashboard = service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO, PortalCardType.MESSAGE));
        int markedCount = snapshotRepository.markStale(
                PortalSnapshotScope.ofIdentity("tenant-1", "person-1", "assignment-1", "position-1"),
                EnumSet.of(PortalCardType.TODO),
                "manual-test",
                FIXED_TIME.plusSeconds(60)
        );
        PortalDashboardView staleDashboard = service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO));

        assertThat(markedCount).isEqualTo(1);
        assertThat(todoCalls).hasValue(2);
        assertThat(messageCalls).hasValue(1);
        assertThat(secondDashboard.todo()).isEqualTo(firstDashboard.todo());
        assertThat(staleDashboard.todo().state()).isEqualTo(PortalCardState.READY);
        assertThat(publishedEvents)
                .hasSize(4)
                .allMatch(PortalSnapshotRefreshedEvent.class::isInstance);

        PortalCardSnapshot<?> refreshedTodoCardSnapshot = service.refreshCard(PortalSceneType.HOME, PortalCardType.TODO);

        assertThat(refreshedTodoCardSnapshot.state()).isEqualTo(PortalCardState.READY);
        assertThat(todoCalls).hasValue(3);
        assertThat(publishedEvents).hasSize(5);
        assertThat(publishedEvents.get(4)).isInstanceOf(PortalSnapshotRefreshedEvent.class);
    }

    @Test
    void shouldConvertStaleSnapshotToFailedWhenRebuildFails() {
        AtomicInteger todoCalls = new AtomicInteger();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        InMemoryPortalCardSnapshotRepository snapshotRepository = new InMemoryPortalCardSnapshotRepository();
        PortalDashboardAggregationApplicationService service = new PortalDashboardAggregationApplicationService(
                identityProvider(),
                () -> {
                    if (todoCalls.getAndIncrement() == 0) {
                        return todoCard();
                    }
                    throw new IllegalStateException("todo unavailable");
                },
                this::messageCard,
                snapshotRepository,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO));
        int markedCount = snapshotRepository.markStale(
                PortalSnapshotScope.ofIdentity("tenant-1", "person-1", "assignment-1", "position-1"),
                EnumSet.of(PortalCardType.TODO),
                "manual-test",
                FIXED_TIME.plusSeconds(60)
        );

        PortalDashboardView dashboard = service.dashboard(PortalSceneType.HOME, EnumSet.of(PortalCardType.TODO));

        assertThat(markedCount).isEqualTo(1);
        assertThat(todoCalls).hasValue(2);
        assertThat(dashboard.todo().state()).isEqualTo(PortalCardState.FAILED);
        assertThat(publishedEvents.get(publishedEvents.size() - 1)).isInstanceOf(PortalSnapshotFailedEvent.class);
    }

    private PortalIdentityCardDataProvider identityProvider() {
        return () -> new PortalIdentityCard(
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

    private PortalTodoCard todoCard() {
        return new PortalTodoCard(
                2,
                1,
                Map.of("approval", 2L),
                java.util.List.of(
                        new PortalTodoItem("todo-1", "Approve travel request", "approval", "HIGH", FIXED_TIME.plusSeconds(3600), FIXED_TIME.minusSeconds(300)),
                        new PortalTodoItem("todo-2", "Review contract", "approval", "NORMAL", FIXED_TIME.plusSeconds(7200), FIXED_TIME.minusSeconds(600))
                )
        );
    }

    private PortalMessageCard messageCard() {
        return new PortalMessageCard(
                2,
                Map.of("TODO_CREATED", 1L, "TODO_OVERDUE", 1L),
                java.util.List.of(
                        new PortalMessageItem("notification-1", "Overdue: Approve travel request", "TODO_OVERDUE", "CRITICAL", "/portal/todo/todo-1", FIXED_TIME.minusSeconds(60)),
                        new PortalMessageItem("notification-2", "Approve travel request", "TODO_CREATED", "HIGH", "/portal/todo/todo-1", FIXED_TIME.minusSeconds(120))
                )
        );
    }
}
