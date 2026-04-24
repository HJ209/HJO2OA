package com.hjo2oa.portal.portal.model.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceState;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatus;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalWidgetReferenceStatusApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T08:30:00Z");

    @Test
    void shouldMarkWidgetReferenceAsStaleWhenWidgetUpdated() {
        PortalWidgetReferenceStatusApplicationService applicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());

        PortalWidgetReferenceStatus status = applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                List.of("displayName", "allowHide")
        ));

        assertThat(status.state()).isEqualTo(PortalWidgetReferenceState.STALE);
        assertThat(status.changedFields()).containsExactly("displayName", "allowHide");
        assertThat(applicationService.current("widget-1")).contains(status);
    }

    @Test
    void shouldMarkWidgetReferenceAsRepairRequiredWhenWidgetDisabled() {
        PortalWidgetReferenceStatusApplicationService applicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());

        PortalWidgetReferenceStatus status = applicationService.markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
        ));

        assertThat(status.state()).isEqualTo(PortalWidgetReferenceState.REPAIR_REQUIRED);
        assertThat(status.changedFields()).isEmpty();
        assertThat(applicationService.current("widget-2")).contains(status);
    }
}
