package com.hjo2oa.portal.widget.config.infrastructure;

import com.hjo2oa.portal.widget.config.domain.WidgetDefinition;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryWidgetDefinitionRepository implements WidgetDefinitionRepository {

    private final Map<String, WidgetDefinition> definitionsById = new ConcurrentHashMap<>();

    @Override
    public Optional<WidgetDefinition> findByWidgetId(String widgetId) {
        return Optional.ofNullable(definitionsById.get(widgetId));
    }

    @Override
    public Optional<WidgetDefinition> findByWidgetCode(String tenantId, String widgetCode) {
        return definitionsById.values().stream()
                .filter(widgetDefinition -> widgetDefinition.tenantId().equals(tenantId))
                .filter(widgetDefinition -> widgetDefinition.widgetCode().equals(widgetCode))
                .findFirst();
    }

    @Override
    public List<WidgetDefinition> findAllByTenant(String tenantId) {
        return definitionsById.values().stream()
                .filter(widgetDefinition -> widgetDefinition.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public WidgetDefinition save(WidgetDefinition widgetDefinition) {
        definitionsById.put(widgetDefinition.widgetId(), widgetDefinition);
        return widgetDefinition;
    }
}
