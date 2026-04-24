package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.application.ReportEventDrivenRefreshApplicationService;
import com.hjo2oa.data.report.application.ReportRefreshApplicationService;
import com.hjo2oa.data.report.domain.DataReportRefreshedEvent;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReportRefreshTriggerListener {

    private final ReportRefreshApplicationService reportRefreshApplicationService;
    private final ReportEventDrivenRefreshApplicationService reportEventDrivenRefreshApplicationService;

    public ReportRefreshTriggerListener(
            ReportRefreshApplicationService reportRefreshApplicationService,
            ReportEventDrivenRefreshApplicationService reportEventDrivenRefreshApplicationService
    ) {
        this.reportRefreshApplicationService = Objects.requireNonNull(
                reportRefreshApplicationService,
                "reportRefreshApplicationService must not be null");
        this.reportEventDrivenRefreshApplicationService = Objects.requireNonNull(
                reportEventDrivenRefreshApplicationService,
                "reportEventDrivenRefreshApplicationService must not be null");
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event instanceof DataReportRefreshedEvent) {
            return;
        }
        if (event.eventType().startsWith("infra.scheduler.")) {
            reportRefreshApplicationService.refreshDueScheduledReports(event.eventType());
            return;
        }
        reportEventDrivenRefreshApplicationService.refreshForDomainEvent(event);
    }
}
