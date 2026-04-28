package com.hjo2oa.portal.aggregation.api.application;

import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterNavItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.TodoCounts;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PortalOfficeCenterAggregationApplicationService {

    private final PortalDashboardAggregationApplicationService dashboardAggregationApplicationService;
    private final TodoQueryApplicationService todoQueryApplicationService;
    private final MessageNotificationQueryApplicationService messageNotificationQueryApplicationService;
    private final Clock clock;
    @Autowired
    public PortalOfficeCenterAggregationApplicationService(
            PortalDashboardAggregationApplicationService dashboardAggregationApplicationService,
            TodoQueryApplicationService todoQueryApplicationService,
            MessageNotificationQueryApplicationService messageNotificationQueryApplicationService
    ) {
        this(
                dashboardAggregationApplicationService,
                todoQueryApplicationService,
                messageNotificationQueryApplicationService,
                Clock.systemUTC()
        );
    }
    public PortalOfficeCenterAggregationApplicationService(
            PortalDashboardAggregationApplicationService dashboardAggregationApplicationService,
            TodoQueryApplicationService todoQueryApplicationService,
            MessageNotificationQueryApplicationService messageNotificationQueryApplicationService,
            Clock clock
    ) {
        this.dashboardAggregationApplicationService = Objects.requireNonNull(
                dashboardAggregationApplicationService,
                "dashboardAggregationApplicationService must not be null"
        );
        this.todoQueryApplicationService = Objects.requireNonNull(
                todoQueryApplicationService,
                "todoQueryApplicationService must not be null"
        );
        this.messageNotificationQueryApplicationService = Objects.requireNonNull(
                messageNotificationQueryApplicationService,
                "messageNotificationQueryApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalOfficeCenterView officeCenter() {
        PortalDashboardView dashboard = dashboardAggregationApplicationService.dashboard(
                PortalSceneType.OFFICE_CENTER,
                EnumSet.of(PortalCardType.TODO, PortalCardType.MESSAGE)
        );
        TodoCounts todoCounts = todoQueryApplicationService.counts();
        NotificationUnreadSummary unreadSummary = messageNotificationQueryApplicationService.unreadSummary();

        return new PortalOfficeCenterView(
                PortalSceneType.OFFICE_CENTER,
                List.of(
                        new PortalOfficeCenterNavItem("pending", "Pending", todoCounts.pendingCount(), "/api/v1/todo/pending"),
                        new PortalOfficeCenterNavItem("completed", "Completed", todoCounts.completedCount(), "/api/v1/todo/completed"),
                        new PortalOfficeCenterNavItem("overdue", "Overdue", todoCounts.overdueCount(), "/api/v1/todo/overdue"),
                        new PortalOfficeCenterNavItem(
                                "messages",
                                "Messages",
                                unreadSummary.totalUnreadCount(),
                                "/api/v1/portal/aggregation/office-center/messages"
                        )
                ),
                dashboard.identity(),
                dashboard.todo(),
                dashboard.message(),
                clock.instant()
        );
    }
}
