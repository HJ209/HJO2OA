package com.hjo2oa.portal.widget.config.domain;

import java.util.List;
import java.util.Optional;

public interface WidgetDefinitionRepository {

    Optional<WidgetDefinition> findByWidgetId(String widgetId);

    Optional<WidgetDefinition> findByWidgetCode(String tenantId, String widgetCode);

    List<WidgetDefinition> findAllByTenant(String tenantId);

    WidgetDefinition save(WidgetDefinition widgetDefinition);
}
