package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiDeprecatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiPublishedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataConnectorUpdatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataReportRefreshedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataServiceActivatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncCompletedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncFailedEvent;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
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
    public void onGenericDataDomainEvent(DomainEvent event) {
        if (event instanceof GovernanceContractEvents.DataApiPublishedEvent
                || event instanceof GovernanceContractEvents.DataApiDeprecatedEvent) {
            return;
        }
        if (DataApiPublishedEvent.EVENT_TYPE.equals(event.eventType())) {
            applicationService.handleDataApiPublished(new DataApiPublishedEvent(
                    event.eventId(),
                    event.occurredAt(),
                    event.tenantId(),
                    requireText(event, "apiId"),
                    requireText(event, "code"),
                    requireText(event, "version"),
                    requireText(event, "path"),
                    requireText(event, "httpMethod"),
                    RuntimeTargetStatus.ACTIVE
            ));
        } else if (DataApiDeprecatedEvent.EVENT_TYPE.equals(event.eventType())) {
            applicationService.handleDataApiDeprecated(new DataApiDeprecatedEvent(
                    event.eventId(),
                    event.occurredAt(),
                    event.tenantId(),
                    requireText(event, "apiId"),
                    requireText(event, "code"),
                    requireText(event, "version"),
                    readInstant(event, "sunsetAt")
            ));
        }
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

    private String requireText(DomainEvent event, String accessorName) {
        Object value = invokeAccessor(event, accessorName);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Domain event " + event.eventType() + " missing " + accessorName);
        }
        return value.toString();
    }

    private Instant readInstant(DomainEvent event, String accessorName) {
        Object value = invokeAccessor(event, accessorName);
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalArgumentException("Domain event " + event.eventType() + " has non-Instant " + accessorName);
    }

    private Object invokeAccessor(DomainEvent event, String accessorName) {
        try {
            return event.getClass().getMethod(accessorName).invoke(event);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Domain event " + event.eventType() + " missing " + accessorName, ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot read domain event " + accessorName, ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Domain event accessor failed: " + accessorName, ex.getCause());
        }
    }
}
