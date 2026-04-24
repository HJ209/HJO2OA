package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.application.ReportRefreshApplicationService;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportScheduledRefreshJob {

    private final ReportRefreshApplicationService reportRefreshApplicationService;

    public ReportScheduledRefreshJob(ReportRefreshApplicationService reportRefreshApplicationService) {
        this.reportRefreshApplicationService = Objects.requireNonNull(
                reportRefreshApplicationService,
                "reportRefreshApplicationService must not be null");
    }

    @Scheduled(fixedDelayString = "${hjo2oa.report.refresh.scan-interval-ms:60000}")
    public void refreshDueReports() {
        reportRefreshApplicationService.refreshDueScheduledReports("scheduler.scan");
    }
}
