package com.hjo2oa.infra.i18n.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record LocaleBundle(
        UUID id,
        String bundleCode,
        String moduleCode,
        String locale,
        String fallbackLocale,
        LocaleBundleStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<LocaleResourceEntry> entries
) {

    private static final Comparator<LocaleResourceEntry> ENTRY_ORDER =
            Comparator.comparing(LocaleResourceEntry::resourceKey);

    public LocaleBundle {
        Objects.requireNonNull(id, "id must not be null");
        bundleCode = requireText(bundleCode, "bundleCode");
        moduleCode = requireText(moduleCode, "moduleCode");
        locale = normalizeLocale(locale, "locale");
        fallbackLocale = normalizeLocaleNullable(fallbackLocale);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (fallbackLocale != null && locale.equals(fallbackLocale)) {
            throw new IllegalArgumentException("fallbackLocale must be different from locale");
        }
        entries = immutableEntries(id, entries);
    }

    public static LocaleBundle create(
            UUID id,
            String bundleCode,
            String moduleCode,
            String locale,
            String fallbackLocale,
            UUID tenantId,
            Instant now
    ) {
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                LocaleBundleStatus.DRAFT,
                tenantId,
                now,
                now,
                List.of()
        );
    }

    public LocaleBundle activate(Instant now) {
        if (status == LocaleBundleStatus.ACTIVE) {
            return this;
        }
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                LocaleBundleStatus.ACTIVE,
                tenantId,
                createdAt,
                now,
                entries
        );
    }

    public LocaleBundle deprecate(Instant now) {
        if (status == LocaleBundleStatus.DEPRECATED) {
            return this;
        }
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                LocaleBundleStatus.DEPRECATED,
                tenantId,
                createdAt,
                now,
                entries
        );
    }

    public LocaleBundle addEntry(String resourceKey, String resourceValue, Instant now) {
        String normalizedKey = requireText(resourceKey, "resourceKey");
        Objects.requireNonNull(resourceValue, "resourceValue must not be null");
        List<LocaleResourceEntry> updatedEntries = new ArrayList<>(entries);
        Optional<LocaleResourceEntry> existing = findEntry(normalizedKey);
        if (existing.isPresent()) {
            LocaleResourceEntry current = existing.get();
            int index = updatedEntries.indexOf(current);
            if (current.active()) {
                throw new IllegalArgumentException("resourceKey already exists: " + normalizedKey);
            }
            updatedEntries.set(index, current.reactivate(resourceValue));
        } else {
            updatedEntries.add(LocaleResourceEntry.create(UUID.randomUUID(), id, normalizedKey, resourceValue));
        }
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                status,
                tenantId,
                createdAt,
                now,
                updatedEntries
        );
    }

    public LocaleBundle updateEntry(String resourceKey, String resourceValue, Instant now) {
        LocaleResourceEntry entry = findActiveEntry(resourceKey)
                .orElseThrow(() -> new IllegalArgumentException("resourceKey not found: " + resourceKey));
        List<LocaleResourceEntry> updatedEntries = replaceEntry(entry, entry.updateValue(resourceValue));
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                status,
                tenantId,
                createdAt,
                now,
                updatedEntries
        );
    }

    public LocaleBundle removeEntry(String resourceKey, Instant now) {
        LocaleResourceEntry entry = findActiveEntry(resourceKey)
                .orElseThrow(() -> new IllegalArgumentException("resourceKey not found: " + resourceKey));
        List<LocaleResourceEntry> updatedEntries = replaceEntry(entry, entry.deactivate());
        return new LocaleBundle(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                status,
                tenantId,
                createdAt,
                now,
                updatedEntries
        );
    }

    public Optional<LocaleResourceEntry> findActiveEntry(String resourceKey) {
        String normalizedKey = requireText(resourceKey, "resourceKey");
        return entries.stream()
                .filter(entry -> entry.resourceKey().equals(normalizedKey))
                .filter(LocaleResourceEntry::active)
                .findFirst();
    }

    public LocaleBundleView toView() {
        return new LocaleBundleView(
                id,
                bundleCode,
                moduleCode,
                locale,
                fallbackLocale,
                status,
                tenantId,
                createdAt,
                updatedAt,
                entries.stream()
                        .filter(LocaleResourceEntry::active)
                        .sorted(ENTRY_ORDER)
                        .map(LocaleResourceEntry::toView)
                        .toList()
        );
    }

    private Optional<LocaleResourceEntry> findEntry(String resourceKey) {
        return entries.stream()
                .filter(entry -> entry.resourceKey().equals(resourceKey))
                .findFirst();
    }

    private List<LocaleResourceEntry> replaceEntry(LocaleResourceEntry source, LocaleResourceEntry target) {
        List<LocaleResourceEntry> updatedEntries = new ArrayList<>(entries.size());
        for (LocaleResourceEntry entry : entries) {
            updatedEntries.add(entry.equals(source) ? target : entry);
        }
        return updatedEntries;
    }

    private static List<LocaleResourceEntry> immutableEntries(UUID bundleId, List<LocaleResourceEntry> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<LocaleResourceEntry> normalized = new ArrayList<>(values.size());
        Set<String> keys = new LinkedHashSet<>();
        for (LocaleResourceEntry value : values) {
            if (value == null) {
                continue;
            }
            if (!bundleId.equals(value.localeBundleId())) {
                throw new IllegalArgumentException("entry localeBundleId must match aggregate id");
            }
            if (!keys.add(value.resourceKey())) {
                throw new IllegalArgumentException("duplicate resourceKey: " + value.resourceKey());
            }
            normalized.add(value);
        }
        normalized.sort(ENTRY_ORDER);
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeLocale(String value, String fieldName) {
        return requireText(value, fieldName).replace('_', '-').toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeLocaleNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalizeLocale(normalized, "fallbackLocale");
    }
}
