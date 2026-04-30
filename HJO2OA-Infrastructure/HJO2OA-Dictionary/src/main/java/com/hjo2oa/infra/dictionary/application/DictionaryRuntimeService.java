package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.cache.application.CacheRuntimeService;
import com.hjo2oa.infra.dictionary.domain.DictionaryItemView;
import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DictionaryRuntimeService {

    private final DictionaryTypeApplicationService dictionaryTypeApplicationService;
    private final CacheRuntimeService cacheRuntimeService;
    private final Duration ttl;

    public DictionaryRuntimeService(
            DictionaryTypeApplicationService dictionaryTypeApplicationService,
            CacheRuntimeService cacheRuntimeService,
            @Value("${hjo2oa.dictionary.cache.ttl-seconds:300}") long ttlSeconds
    ) {
        this.dictionaryTypeApplicationService = Objects.requireNonNull(
                dictionaryTypeApplicationService,
                "dictionaryTypeApplicationService must not be null"
        );
        this.cacheRuntimeService = Objects.requireNonNull(cacheRuntimeService, "cacheRuntimeService must not be null");
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public RuntimeDictionaryView query(UUID tenantId, String code, boolean enabledOnly, boolean tree, String language) {
        DictionaryTypeView dictionaryType = loadDictionaryType(tenantId, code);
        String normalizedLanguage = normalizeLanguage(language);
        String key = cacheKey(dictionaryType.code(), enabledOnly, tree, normalizedLanguage);
        if (!dictionaryType.cacheable()) {
            return buildRuntimeView(dictionaryType, enabledOnly, tree, normalizedLanguage);
        }
        return cacheRuntimeService.getOrLoad(
                DictionaryCacheService.NAMESPACE,
                tenantId,
                key,
                ttl,
                RuntimeDictionaryView.class,
                () -> buildRuntimeView(dictionaryType, enabledOnly, tree, normalizedLanguage)
        );
    }

    public Map<String, RuntimeDictionaryView> batch(
            UUID tenantId,
            List<String> codes,
            boolean enabledOnly,
            boolean tree,
            String language
    ) {
        Objects.requireNonNull(codes, "codes must not be null");
        Map<String, RuntimeDictionaryView> result = new LinkedHashMap<>();
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            result.put(code, query(tenantId, code, enabledOnly, tree, language));
        }
        return result;
    }

    public RuntimeDictionaryView refresh(UUID tenantId, String code, boolean tree, String language) {
        DictionaryTypeView dictionaryType = loadDictionaryType(tenantId, code);
        cacheRuntimeService.evictByPrefix(DictionaryCacheService.NAMESPACE, tenantId, dictionaryType.code() + ":");
        return query(tenantId, code, true, tree, language);
    }

    private DictionaryTypeView loadDictionaryType(UUID tenantId, String code) {
        return dictionaryTypeApplicationService.queryByCode(tenantId, code)
                .filter(type -> type.status() == DictionaryStatus.ACTIVE)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Dictionary type not found"
                ));
    }

    private RuntimeDictionaryView buildRuntimeView(
            DictionaryTypeView dictionaryType,
            boolean enabledOnly,
            boolean tree,
            String language
    ) {
        List<RuntimeItemView> flatItems = dictionaryType.items().stream()
                .filter(item -> !enabledOnly || item.enabled())
                .sorted(Comparator.comparingInt(DictionaryItemView::sortOrder)
                        .thenComparing(DictionaryItemView::itemCode))
                .map(item -> toRuntimeItem(item, List.of()))
                .toList();
        List<RuntimeItemView> items = tree ? toTree(flatItems) : flatItems;
        return new RuntimeDictionaryView(
                dictionaryType.id(),
                dictionaryType.code(),
                dictionaryType.name(),
                dictionaryType.category(),
                dictionaryType.hierarchical(),
                dictionaryType.tenantId(),
                language,
                items
        );
    }

    private RuntimeItemView toRuntimeItem(DictionaryItemView item, List<RuntimeItemView> children) {
        return new RuntimeItemView(
                item.id(),
                item.itemCode(),
                item.displayName(),
                item.multiLangValue() == null ? item.itemCode() : item.multiLangValue(),
                item.parentItemId(),
                item.sortOrder(),
                item.enabled(),
                item.defaultItem(),
                item.extensionJson(),
                children
        );
    }

    private List<RuntimeItemView> toTree(List<RuntimeItemView> flatItems) {
        Map<UUID, List<RuntimeItemView>> childrenByParent = new LinkedHashMap<>();
        for (RuntimeItemView item : flatItems) {
            childrenByParent.computeIfAbsent(item.parentId(), ignored -> new ArrayList<>()).add(item);
        }
        return buildChildren(null, childrenByParent);
    }

    private List<RuntimeItemView> buildChildren(UUID parentId, Map<UUID, List<RuntimeItemView>> childrenByParent) {
        return childrenByParent.getOrDefault(parentId, List.of()).stream()
                .map(item -> new RuntimeItemView(
                        item.id(),
                        item.code(),
                        item.label(),
                        item.value(),
                        item.parentId(),
                        item.sortOrder(),
                        item.enabled(),
                        item.defaultItem(),
                        item.extensionJson(),
                        buildChildren(item.id(), childrenByParent)
                ))
                .toList();
    }

    private static String cacheKey(String code, boolean enabledOnly, boolean tree, String language) {
        return code + ":" + (tree ? "tree" : "items") + ":enabled=" + enabledOnly + ":lang=" + language;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return Locale.SIMPLIFIED_CHINESE.toLanguageTag();
        }
        return language.trim();
    }

    public record RuntimeItemView(
            UUID id,
            String code,
            String label,
            String value,
            UUID parentId,
            int sortOrder,
            boolean enabled,
            boolean defaultItem,
            String extensionJson,
            List<RuntimeItemView> children
    ) implements Serializable {
    }

    public record RuntimeDictionaryView(
            UUID id,
            String code,
            String name,
            String category,
            boolean hierarchical,
            UUID tenantId,
            String language,
            List<RuntimeItemView> items
    ) implements Serializable {
    }
}
