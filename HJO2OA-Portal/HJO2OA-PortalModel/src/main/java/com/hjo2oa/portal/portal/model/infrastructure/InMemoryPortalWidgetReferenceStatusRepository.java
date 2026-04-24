package com.hjo2oa.portal.portal.model.infrastructure;

import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatus;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatusRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalWidgetReferenceStatusRepository implements PortalWidgetReferenceStatusRepository {

    private final Map<String, PortalWidgetReferenceStatus> statusesByWidgetId = new ConcurrentHashMap<>();
    private final Map<String, PortalWidgetReferenceStatus> statusesByTenantAndWidgetCode = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalWidgetReferenceStatus> findByWidgetId(String widgetId) {
        return Optional.ofNullable(statusesByWidgetId.get(widgetId));
    }

    @Override
    public Optional<PortalWidgetReferenceStatus> findByWidgetCode(String tenantId, String widgetCode) {
        return Optional.ofNullable(statusesByTenantAndWidgetCode.get(keyOf(tenantId, widgetCode)));
    }

    @Override
    public PortalWidgetReferenceStatus save(PortalWidgetReferenceStatus status) {
        statusesByWidgetId.put(status.widgetId(), status);
        statusesByTenantAndWidgetCode.put(keyOf(status.tenantId(), status.widgetCode()), status);
        return status;
    }

    private String keyOf(String tenantId, String widgetCode) {
        return tenantId + "::" + widgetCode;
    }
}
