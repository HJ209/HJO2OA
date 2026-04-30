package com.hjo2oa.infra.dictionary.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DictionaryType(
        UUID id,
        String code,
        String name,
        String category,
        boolean hierarchical,
        boolean cacheable,
        int sortOrder,
        boolean systemManaged,
        DictionaryStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<DictionaryItem> items
) {

    public DictionaryType {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        category = normalizeNullableText(category);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must not be negative");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        items = sortItems(items == null ? List.of() : items);
        validateItems(id, hierarchical, items);
    }

    public static DictionaryType create(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            UUID tenantId,
            Instant now
    ) {
        return create(code, name, category, hierarchical, cacheable, 0, false, tenantId, now);
    }

    public static DictionaryType create(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            int sortOrder,
            boolean systemManaged,
            UUID tenantId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new DictionaryType(
                UUID.randomUUID(),
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder,
                systemManaged,
                DictionaryStatus.ACTIVE,
                tenantId,
                now,
                now,
                List.of()
        );
    }

    public DictionaryType update(
            String newName,
            String newCategory,
            Boolean newHierarchical,
            Boolean newCacheable,
            Integer newSortOrder,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new DictionaryType(
                id,
                code,
                newName == null ? name : newName,
                newCategory == null ? category : newCategory,
                newHierarchical == null ? hierarchical : newHierarchical,
                newCacheable == null ? cacheable : newCacheable,
                newSortOrder == null ? sortOrder : newSortOrder,
                systemManaged,
                status,
                tenantId,
                createdAt,
                now,
                items
        );
    }

    public DictionaryType disable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == DictionaryStatus.DISABLED) {
            return this;
        }
        return new DictionaryType(
                id,
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder,
                systemManaged,
                DictionaryStatus.DISABLED,
                tenantId,
                createdAt,
                now,
                items
        );
    }

    public DictionaryType enable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == DictionaryStatus.ACTIVE) {
            return this;
        }
        return new DictionaryType(
                id,
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder,
                systemManaged,
                DictionaryStatus.ACTIVE,
                tenantId,
                createdAt,
                now,
                items
        );
    }

    public DictionaryType addItem(
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            Instant now
    ) {
        return addItem(itemCode, displayName, parentItemId, sortOrder, false, null, null, now);
    }

    public DictionaryType addItem(
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            boolean defaultItem,
            String multiLangValue,
            String extensionJson,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        ensureItemCodeUnique(itemCode);
        validateParent(parentItemId);
        List<DictionaryItem> mutableItems = new ArrayList<>(items);
        mutableItems.add(new DictionaryItem(
                UUID.randomUUID(),
                id,
                itemCode,
                displayName,
                parentItemId,
                sortOrder == null ? 0 : sortOrder,
                true,
                multiLangValue,
                defaultItem,
                extensionJson
        ));
        return withItems(normalizeDefaultItems(mutableItems), now);
    }

    public DictionaryType upsertItem(
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            boolean enabled,
            boolean defaultItem,
            String multiLangValue,
            String extensionJson,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        String normalizedCode = requireText(itemCode, "itemCode");
        DictionaryItem existingItem = items.stream()
                .filter(item -> item.itemCode().equals(normalizedCode))
                .findFirst()
                .orElse(null);
        if (existingItem == null) {
            DictionaryType withNewItem = addItem(
                    normalizedCode,
                    displayName,
                    parentItemId,
                    sortOrder,
                    defaultItem,
                    multiLangValue,
                    extensionJson,
                    now
            );
            DictionaryItem addedItem = withNewItem.items().stream()
                    .filter(item -> item.itemCode().equals(normalizedCode))
                    .findFirst()
                    .orElseThrow();
            return withNewItem.replaceItem(enabled ? addedItem.enable() : addedItem.disable(), now);
        }
        DictionaryItem updatedItem = existingItem.update(
                displayName,
                parentItemId == null ? existingItem.parentItemId() : parentItemId,
                sortOrder,
                defaultItem,
                multiLangValue,
                extensionJson
        );
        updatedItem = enabled ? updatedItem.enable() : updatedItem.disable();
        return replaceItem(updatedItem, now);
    }

    public DictionaryType removeItem(UUID itemId, Instant now) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        findItem(itemId);
        if (items.stream().anyMatch(item -> itemId.equals(item.parentItemId()))) {
            throw new IllegalArgumentException("Cannot remove dictionary item that still has children");
        }
        List<DictionaryItem> mutableItems = items.stream()
                .filter(item -> !item.id().equals(itemId))
                .toList();
        return withItems(mutableItems, now);
    }

    public DictionaryType updateItem(UUID itemId, String displayName, Integer sortOrder, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        DictionaryItem existingItem = findItem(itemId);
        return replaceItem(existingItem.update(displayName, sortOrder), now);
    }

    public DictionaryType updateItem(
            UUID itemId,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            Boolean defaultItem,
            String multiLangValue,
            String extensionJson,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        DictionaryItem existingItem = findItem(itemId);
        UUID resolvedParentItemId = parentItemId == null ? existingItem.parentItemId() : parentItemId;
        validateParentForUpdate(itemId, resolvedParentItemId);
        return replaceItem(
                existingItem.update(
                        displayName,
                        resolvedParentItemId,
                        sortOrder,
                        defaultItem,
                        multiLangValue,
                        extensionJson
                ),
                now
        );
    }

    public DictionaryType enableItem(UUID itemId, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        DictionaryItem existingItem = findItem(itemId);
        return replaceItem(existingItem.enable(), now);
    }

    public DictionaryType disableItem(UUID itemId, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        DictionaryItem existingItem = findItem(itemId);
        return replaceItem(existingItem.disable(), now);
    }

    public DictionaryTypeView toView() {
        return new DictionaryTypeView(
                id,
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder,
                systemManaged,
                status,
                tenantId,
                createdAt,
                updatedAt,
                items.stream().map(DictionaryItem::toView).toList()
        );
    }

    private DictionaryType replaceItem(DictionaryItem updatedItem, Instant now) {
        List<DictionaryItem> mutableItems = items.stream()
                .map(item -> item.id().equals(updatedItem.id()) ? updatedItem : item)
                .toList();
        return withItems(normalizeDefaultItems(mutableItems), now);
    }

    private DictionaryType withItems(List<DictionaryItem> updatedItems, Instant now) {
        return new DictionaryType(
                id,
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder,
                systemManaged,
                status,
                tenantId,
                createdAt,
                now,
                updatedItems
        );
    }

    private DictionaryItem findItem(UUID itemId) {
        return items.stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Dictionary item not found"));
    }

    private void ensureItemCodeUnique(String itemCode) {
        String normalizedCode = requireText(itemCode, "itemCode");
        boolean duplicated = items.stream().anyMatch(item -> item.itemCode().equals(normalizedCode));
        if (duplicated) {
            throw new IllegalArgumentException("Dictionary item code already exists");
        }
    }

    private void validateParent(UUID parentItemId) {
        if (parentItemId == null) {
            return;
        }
        if (!hierarchical) {
            throw new IllegalArgumentException("Parent item is only allowed for hierarchical dictionaries");
        }
        boolean parentExists = items.stream().anyMatch(item -> item.id().equals(parentItemId));
        if (!parentExists) {
            throw new IllegalArgumentException("Parent dictionary item not found");
        }
    }

    private void validateParentForUpdate(UUID itemId, UUID parentItemId) {
        if (parentItemId == null) {
            return;
        }
        if (itemId.equals(parentItemId)) {
            throw new IllegalArgumentException("Dictionary item cannot be its own parent");
        }
        validateParent(parentItemId);
    }

    private static void validateItems(UUID dictionaryTypeId, boolean hierarchical, List<DictionaryItem> validatedItems) {
        Map<UUID, DictionaryItem> itemsById = new HashMap<>();
        Set<String> itemCodes = new HashSet<>();
        int defaultCount = 0;
        for (DictionaryItem item : validatedItems) {
            if (!dictionaryTypeId.equals(item.dictionaryTypeId())) {
                throw new IllegalArgumentException("Dictionary item does not belong to dictionary type");
            }
            if (!itemCodes.add(item.itemCode())) {
                throw new IllegalArgumentException("Dictionary item code already exists");
            }
            if (item.defaultItem()) {
                defaultCount++;
            }
            itemsById.put(item.id(), item);
        }
        if (defaultCount > 1) {
            throw new IllegalArgumentException("Only one default dictionary item is allowed");
        }
        for (DictionaryItem item : validatedItems) {
            UUID parentItemId = item.parentItemId();
            if (parentItemId == null) {
                continue;
            }
            if (!hierarchical) {
                throw new IllegalArgumentException("Parent item is only allowed for hierarchical dictionaries");
            }
            if (!itemsById.containsKey(parentItemId)) {
                throw new IllegalArgumentException("Parent dictionary item not found");
            }
            ensureAcyclic(item.id(), itemsById);
        }
    }

    private static void ensureAcyclic(UUID itemId, Map<UUID, DictionaryItem> itemsById) {
        Set<UUID> visited = new HashSet<>();
        UUID currentItemId = itemId;
        while (currentItemId != null) {
            if (!visited.add(currentItemId)) {
                throw new IllegalArgumentException("Dictionary item hierarchy contains a cycle");
            }
            DictionaryItem currentItem = itemsById.get(currentItemId);
            currentItemId = currentItem == null ? null : currentItem.parentItemId();
        }
    }

    private static List<DictionaryItem> normalizeDefaultItems(List<DictionaryItem> sourceItems) {
        DictionaryItem defaultItem = sourceItems.stream()
                .filter(DictionaryItem::defaultItem)
                .reduce((left, right) -> right)
                .orElse(null);
        if (defaultItem == null) {
            return sourceItems;
        }
        return sourceItems.stream()
                .map(item -> item.id().equals(defaultItem.id()) ? item : item.asNonDefault())
                .toList();
    }

    private static List<DictionaryItem> sortItems(List<DictionaryItem> sourceItems) {
        return List.copyOf(sourceItems.stream()
                .sorted(Comparator.comparingInt(DictionaryItem::sortOrder)
                        .thenComparing(DictionaryItem::itemCode)
                        .thenComparing(item -> item.id().toString()))
                .toList());
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
