package com.hjo2oa.portal.portal.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.portal.portal.home.domain.PortalHomeAggregationViewProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.home.infrastructure.StaticPortalHomePageTemplateProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PortalHomePageAssemblyApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T12:00:00Z");

    @Test
    void shouldAssembleHomePageWithThreeSectionLayout() {
        AtomicReference<Set<PortalCardType>> requestedCards = new AtomicReference<>();
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                capturingProvider(requestedCards, dashboardReady()),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.layoutType().name()).isEqualTo("THREE_SECTION");
        assertThat(page.regions()).hasSize(2);
        assertThat(page.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("IDENTITY");
        assertThat(page.regions().get(1).cards()).extracting(card -> card.cardType().name())
                .containsExactly("TODO", "MESSAGE");
        assertThat(requestedCards.get()).containsExactlyInAnyOrder(
                PortalCardType.IDENTITY,
                PortalCardType.TODO,
                PortalCardType.MESSAGE
        );
    }

    @Test
    void shouldKeepDegradedCardInPageAssembly() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                (sceneType, cardTypes) -> dashboardWithDegradedMessage(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.HOME);

        assertThat(page.regions().get(1).cards()).anySatisfy(card -> {
            if (card.cardType() == PortalCardType.MESSAGE) {
                assertThat(card.state().name()).isEqualTo("FAILED");
                assertThat(card.message()).isEqualTo("Message card is temporarily unavailable");
            }
        });
    }

    @Test
    void shouldAssembleMobileWorkbenchWithMobileLayout() {
        PortalHomePageAssemblyApplicationService service = new PortalHomePageAssemblyApplicationService(
                new StaticPortalHomePageTemplateProvider(),
                (sceneType, cardTypes) -> dashboardReady(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalHomePageView page = service.page(PortalHomeSceneType.MOBILE_WORKBENCH);

        assertThat(page.layoutType().name()).isEqualTo("MOBILE_LIGHT");
        assertThat(page.regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("TODO", "MESSAGE", "IDENTITY"));
    }

    private PortalHomeAggregationViewProvider capturingProvider(
            AtomicReference<Set<PortalCardType>> requestedCards,
            PortalDashboardView response
    ) {
        return (sceneType, cardTypes) -> {
            requestedCards.set(Set.copyOf(cardTypes));
            return response;
        };
    }

    private PortalDashboardView dashboardReady() {
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
        return new PortalDashboardView(
                PortalSceneType.HOME,
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.IDENTITY),
                        PortalCardType.IDENTITY,
                        identity,
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.TODO),
                        PortalCardType.TODO,
                        new PortalTodoCard(
                                1,
                                1,
                                Map.of("approval", 1L),
                                java.util.List.of(new PortalTodoItem(
                                        "todo-1",
                                        "Approve budget",
                                        "approval",
                                        "HIGH",
                                        FIXED_TIME.plusSeconds(3600),
                                        FIXED_TIME.minusSeconds(600)
                                ))
                        ),
                        FIXED_TIME
                ),
                PortalCardSnapshot.ready(
                        PortalAggregationSnapshotKey.of(identity, PortalSceneType.HOME, PortalCardType.MESSAGE),
                        PortalCardType.MESSAGE,
                        new PortalMessageCard(
                                1,
                                Map.of("TODO_CREATED", 1L),
                                java.util.List.of(new PortalMessageItem(
                                        "notification-1",
                                        "Approve budget",
                                        "TODO_CREATED",
                                        "HIGH",
                                        "/portal/todo/todo-1",
                                        FIXED_TIME.minusSeconds(300)
                                ))
                        ),
                        FIXED_TIME
                )
        );
    }

    private PortalDashboardView dashboardWithDegradedMessage() {
        PortalDashboardView readyDashboard = dashboardReady();
        return new PortalDashboardView(
                readyDashboard.sceneType(),
                readyDashboard.identity(),
                readyDashboard.todo(),
                PortalCardSnapshot.failed(
                        readyDashboard.message().snapshotKey(),
                        PortalCardType.MESSAGE,
                        PortalMessageCard.empty(),
                        "Message card is temporarily unavailable",
                        FIXED_TIME
                )
        );
    }
}
