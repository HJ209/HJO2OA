package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiDeprecatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiPublishedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataConnectorUpdatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataReportRefreshedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataServiceActivatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncCompletedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GovernanceRuntimeEventListener {

    private final GovernanceMonitoringApplicationService applicationService;

    public GovernanceRuntimeEventListener(GovernanceMonitoringApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @EventListener
    public void onApiPublished(DataApiPublishedEvent event) {
        applicationService.handleDataApiPublished(event);
    }

    @EventListener
    public void onApiDeprecated(DataApiDeprecatedEvent event) {
        applicationService.handleDataApiDeprecated(event);
    }

    @EventListener
    public void onConnectorUpdated(DataConnectorUpdatedEvent event) {
        applicationService.handleDataConnectorUpdated(event);
    }

    @EventListener
    public void onSyncCompleted(DataSyncCompletedEvent event) {
        applicationService.handleDataSyncCompleted(event);
    }

    @EventListener
    public void onSyncFailed(DataSyncFailedEvent event) {
        applicationService.handleDataSyncFailed(event);
    }

    @EventListener
    public void onReportRefreshed(DataReportRefreshedEvent event) {
        applicationService.handleDataReportRefreshed(event);
    }

    @EventListener
    public void onServiceActivated(DataServiceActivatedEvent event) {
        applicationService.handleDataServiceActivated(event);
    }
}
