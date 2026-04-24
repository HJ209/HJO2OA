package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GovernanceHealthCheckScheduler {

    private final GovernanceMonitoringApplicationService applicationService;

    public GovernanceHealthCheckScheduler(GovernanceMonitoringApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Scheduled(cron = "${hjo2oa.data.governance.health-check-cron:0 */5 * * * *}")
    public void scheduleHealthChecks() {
        applicationService.runScheduledHealthChecks();
    }
}
