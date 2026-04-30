package com.hjo2oa.infra.cache.infrastructure;

import com.hjo2oa.infra.cache.application.CacheRuntimeService;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CacheDomainEventInvalidationListener {

    public static final String DICTIONARY_NAMESPACE = "infra.dictionary.runtime";
    public static final String CONFIG_NAMESPACE = "infra.config.runtime";
    public static final String FEATURE_FLAG_NAMESPACE = "infra.feature-flag.runtime";

    private final CacheRuntimeService cacheRuntimeService;

    public CacheDomainEventInvalidationListener(CacheRuntimeService cacheRuntimeService) {
        this.cacheRuntimeService = Objects.requireNonNull(cacheRuntimeService, "cacheRuntimeService must not be null");
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        switch (event.eventType()) {
            case "infra.dictionary.updated" -> invalidateDictionary(event);
            case "infra.config.updated" -> invalidateConfig(event);
            case "infra.feature-flag.changed" -> invalidateFeatureFlag(event);
            default -> {
            }
        }
    }

    private void invalidateDictionary(DomainEvent event) {
        String code = stringValue(event, "dictionaryCode");
        UUID tenantId = tenantUuid(event);
        if (code == null) {
            cacheRuntimeService.evictNamespace(DICTIONARY_NAMESPACE);
            return;
        }
        cacheRuntimeService.evictByPrefix(DICTIONARY_NAMESPACE, tenantId, code + ":");
    }

    private void invalidateConfig(DomainEvent event) {
        String configKey = stringValue(event, "configKey");
        UUID tenantId = tenantUuid(event);
        if (configKey == null) {
            cacheRuntimeService.evictNamespace(CONFIG_NAMESPACE);
            return;
        }
        cacheRuntimeService.evictByPrefix(CONFIG_NAMESPACE, tenantId, configKey + ":");
    }

    private void invalidateFeatureFlag(DomainEvent event) {
        String configKey = stringValue(event, "configKey");
        UUID tenantId = tenantUuid(event);
        if (configKey == null) {
            cacheRuntimeService.evictNamespace(FEATURE_FLAG_NAMESPACE);
            return;
        }
        cacheRuntimeService.evictByPrefix(FEATURE_FLAG_NAMESPACE, tenantId, configKey + ":");
    }

    private static UUID tenantUuid(DomainEvent event) {
        String tenantId = event.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String stringValue(Object source, String methodName) {
        try {
            Method method = source.getClass().getMethod(methodName);
            Object value = method.invoke(source);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
