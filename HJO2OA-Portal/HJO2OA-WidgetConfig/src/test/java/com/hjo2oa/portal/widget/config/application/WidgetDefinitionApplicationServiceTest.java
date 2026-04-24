package com.hjo2oa.portal.widget.config.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionView;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WidgetDefinitionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T04:30:00Z");

    @Test
    void shouldCreateWidgetAndPublishUpdatedEvent() {
        InMemoryWidgetDefinitionRepository repository = new InMemoryWidgetDefinitionRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        WidgetDefinitionApplicationService applicationService = applicationService(repository, publishedEvents);

        WidgetDefinitionView widgetDefinition = applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "todo-card",
                "Todo Card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));

        assertThat(widgetDefinition.widgetId()).isEqualTo("widget-1");
        assertThat(widgetDefinition.widgetCode()).isEqualTo("todo-card");
        assertThat(widgetDefinition.status().name()).isEqualTo("ACTIVE");
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalWidgetUpdatedEvent.class);
        PortalWidgetUpdatedEvent updatedEvent = (PortalWidgetUpdatedEvent) publishedEvents.get(0);
        assertThat(updatedEvent.changedFields()).contains("widgetCode", "cardType", "sceneType");
    }

    @Test
    void shouldDisableWidgetAndPublishDisabledEvent() {
        InMemoryWidgetDefinitionRepository repository = new InMemoryWidgetDefinitionRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        WidgetDefinitionApplicationService applicationService = applicationService(repository, publishedEvents);
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "message-card",
                "Message Card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                5
        ));
        publishedEvents.clear();

        WidgetDefinitionView disabledWidget = applicationService.disable(new DisableWidgetDefinitionCommand("widget-1"));

        assertThat(disabledWidget.status().name()).isEqualTo("DISABLED");
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalWidgetDisabledEvent.class);
    }

    @Test
    void shouldRejectDuplicateWidgetCode() {
        WidgetDefinitionApplicationService applicationService = applicationService(
                new InMemoryWidgetDefinitionRepository(),
                new ArrayList<>()
        );
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "identity-card",
                "Identity Card",
                WidgetCardType.IDENTITY,
                WidgetSceneType.HOME,
                "identity-context",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                false,
                1
        ));

        assertThatThrownBy(() -> applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-2",
                "identity-card",
                "Duplicate Identity Card",
                WidgetCardType.IDENTITY,
                WidgetSceneType.OFFICE_CENTER,
                "identity-context",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                false,
                1
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Widget code already exists");
    }

    @Test
    void shouldListWidgetDefinitionsFilteredBySceneAndStatus() {
        InMemoryWidgetDefinitionRepository repository = new InMemoryWidgetDefinitionRepository();
        WidgetDefinitionApplicationService applicationService = applicationService(repository, new ArrayList<>());
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "home-message",
                "Home Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                5
        ));
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-2",
                "office-todo",
                "Office Todo",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-3",
                "office-message",
                "Office Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.OFFICE_CENTER,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                6
        ));
        applicationService.disable(new DisableWidgetDefinitionCommand("widget-3"));

        assertThat(applicationService.list(null, null))
                .extracting(WidgetDefinitionView::widgetCode)
                .containsExactly("home-message", "office-message", "office-todo");
        assertThat(applicationService.list(WidgetSceneType.HOME, null))
                .singleElement()
                .satisfies(widget -> assertThat(widget.widgetId()).isEqualTo("widget-1"));
        assertThat(applicationService.list(WidgetSceneType.OFFICE_CENTER, WidgetDefinitionStatus.ACTIVE))
                .singleElement()
                .satisfies(widget -> assertThat(widget.widgetId()).isEqualTo("widget-2"));
        assertThat(applicationService.list(null, WidgetDefinitionStatus.DISABLED))
                .singleElement()
                .satisfies(widget -> assertThat(widget.widgetId()).isEqualTo("widget-3"));
    }

    @Test
    void shouldReturnLatestWidgetDefinitionOnceAfterRepeatedUpserts() {
        InMemoryWidgetDefinitionRepository repository = new InMemoryWidgetDefinitionRepository();
        WidgetDefinitionApplicationService applicationService = applicationService(repository, new ArrayList<>());
        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "todo-card",
                "Todo Card",
                WidgetCardType.TODO,
                WidgetSceneType.HOME,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));

        applicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "todo-card",
                "Todo Card V2",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                10
        ));

        assertThat(applicationService.list(null, null))
                .singleElement()
                .satisfies(widget -> {
                    assertThat(widget.displayName()).isEqualTo("Todo Card V2");
                    assertThat(widget.sceneType()).isEqualTo(WidgetSceneType.OFFICE_CENTER);
                    assertThat(widget.maxItems()).isEqualTo(10);
                });
    }

    private WidgetDefinitionApplicationService applicationService(
            InMemoryWidgetDefinitionRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        WidgetConfigContextProvider contextProvider = () -> new WidgetConfigContext("tenant-1", "portal-admin");
        return new WidgetDefinitionApplicationService(
                repository,
                contextProvider,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
