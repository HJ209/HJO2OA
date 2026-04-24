package com.hjo2oa.portal.portal.model.domain;

import java.util.Optional;

public interface PortalWidgetReferenceStatusRepository {

    Optional<PortalWidgetReferenceStatus> findByWidgetId(String widgetId);

    Optional<PortalWidgetReferenceStatus> findByWidgetCode(String tenantId, String widgetCode);

    PortalWidgetReferenceStatus save(PortalWidgetReferenceStatus status);
}
