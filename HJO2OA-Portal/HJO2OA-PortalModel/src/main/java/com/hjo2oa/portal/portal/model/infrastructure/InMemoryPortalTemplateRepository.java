package com.hjo2oa.portal.portal.model.infrastructure;

import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPortalTemplateRepository implements PortalTemplateRepository {

    private final Map<String, PortalTemplate> templates = new ConcurrentHashMap<>();

    @Override
    public Optional<PortalTemplate> findByTemplateId(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    @Override
    public Optional<PortalTemplate> findByTemplateCode(String tenantId, String templateCode) {
        return templates.values().stream()
                .filter(template -> template.tenantId().equals(tenantId))
                .filter(template -> template.templateCode().equals(templateCode))
                .findFirst();
    }

    @Override
    public List<PortalTemplate> findAllByTenant(String tenantId) {
        return templates.values().stream()
                .filter(template -> template.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public PortalTemplate save(PortalTemplate template) {
        templates.put(template.templateId(), template);
        return template;
    }
}
