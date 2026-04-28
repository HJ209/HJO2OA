package com.hjo2oa.wf.process.instance.infrastructure.flowable;

import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hjo2oa.workflow.engine", havingValue = "flowable", matchIfMissing = true)
public class FlowableProcessEngineEventBridge implements FlowableEventListener {

    private static final String EVENT_TYPE_PREFIX = "workflow.flowable.";
    private static final String DEFAULT_TENANT = "flowable";

    private final DomainEventPublisher eventPublisher;

    public FlowableProcessEngineEventBridge(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        FlowableEngineEvent engineEvent = event instanceof FlowableEngineEvent flowableEngineEvent
                ? flowableEngineEvent
                : null;
        Object entity = event instanceof FlowableEngineEntityEvent entityEvent ? entityEvent.getEntity() : null;
        String tenantId = resolveTenantId(event);
        String flowableType = event.getType().toString();
        eventPublisher.publish(new FlowableProcessEngineDomainEvent(
                UUID.randomUUID(),
                EVENT_TYPE_PREFIX + flowableType.toLowerCase(java.util.Locale.ROOT),
                Instant.now(),
                tenantId,
                flowableType,
                engineEvent == null ? null : engineEvent.getProcessDefinitionId(),
                engineEvent == null ? null : engineEvent.getProcessInstanceId(),
                engineEvent == null ? null : engineEvent.getExecutionId(),
                entityId(entity),
                entity == null ? null : entity.getClass().getName()
        ));
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }

    private String resolveTenantId(FlowableEvent event) {
        return DEFAULT_TENANT;
    }

    private String entityId(Object entity) {
        if (entity instanceof org.flowable.task.api.Task task) {
            return task.getId();
        }
        if (entity instanceof org.flowable.engine.runtime.ProcessInstance processInstance) {
            return processInstance.getId();
        }
        if (entity instanceof org.flowable.engine.repository.ProcessDefinition processDefinition) {
            return processDefinition.getId();
        }
        return null;
    }
}
