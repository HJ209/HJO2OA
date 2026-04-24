package com.hjo2oa.infra.data.i18n.application;

import com.hjo2oa.infra.data.i18n.domain.TranslationEntry;
import com.hjo2oa.infra.data.i18n.domain.TranslationEntryRepository;
import com.hjo2oa.infra.data.i18n.domain.TranslationEntryView;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolveSource;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolutionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TranslationEntryApplicationService {

    private static final List<String> DEFAULT_FALLBACK_LOCALES = List.of("zh-CN", "en-US");

    private final TranslationEntryRepository repository;
    private final Clock clock;

    public TranslationEntryApplicationService(TranslationEntryRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public TranslationEntryApplicationService(TranslationEntryRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TranslationEntryView createTranslation(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            String value,
            UUID tenantId
    ) {
        return createTranslation(new TranslationEntryCommands.CreateCommand(
                entityType,
                entityId,
                fieldName,
                locale,
                value,
                tenantId,
                null
        ));
    }

    public TranslationEntryView createTranslation(TranslationEntryCommands.CreateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String entityType = requireText(command.entityType(), "entityType");
        String entityId = requireText(command.entityId(), "entityId");
        String fieldName = requireText(command.fieldName(), "fieldName");
        String locale = normalizeLocale(command.locale());
        Objects.requireNonNull(command.tenantId(), "tenantId must not be null");
        repository.findTranslation(entityType, entityId, fieldName, locale).ifPresent(existing -> {
            throw new BizException(
                    SharedErrorDescriptors.CONFLICT,
                    "translation entry already exists for key: "
                            + uniqueKey(entityType, entityId, fieldName, locale)
            );
        });
        try {
            TranslationEntry entry = TranslationEntry.create(
                    entityType,
                    entityId,
                    fieldName,
                    locale,
                    command.value(),
                    command.tenantId(),
                    command.updatedBy(),
                    now()
            );
            return repository.save(entry).toView();
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public TranslationEntryView updateTranslation(UUID entryId, String value) {
        return updateTranslation(new TranslationEntryCommands.UpdateCommand(entryId, value, null));
    }

    public TranslationEntryView updateTranslation(TranslationEntryCommands.UpdateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TranslationEntry existing = loadEntry(command.entryId());
        try {
            return repository.save(existing.translate(command.value(), command.updatedBy(), now())).toView();
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public TranslationEntryView reviewTranslation(UUID entryId) {
        return reviewTranslation(new TranslationEntryCommands.ReviewCommand(entryId, null));
    }

    public TranslationEntryView reviewTranslation(TranslationEntryCommands.ReviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TranslationEntry existing = loadEntry(command.entryId());
        try {
            return repository.save(existing.review(command.updatedBy(), now())).toView();
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public TranslationResolutionView resolveTranslation(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId
    ) {
        return resolveTranslation(new TranslationEntryCommands.ResolveCommand(
                entityType,
                entityId,
                fieldName,
                locale,
                tenantId,
                null,
                null
        ));
    }

    public TranslationResolutionView resolveTranslation(TranslationEntryCommands.ResolveCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String entityType = requireText(command.entityType(), "entityType");
        String entityId = requireText(command.entityId(), "entityId");
        String fieldName = requireText(command.fieldName(), "fieldName");
        String requestedLocale = normalizeLocale(command.locale());
        Objects.requireNonNull(command.tenantId(), "tenantId must not be null");

        Map<String, TranslationEntry> entriesByLocale = new LinkedHashMap<>();
        for (TranslationEntry entry : repository.findTranslationsByEntity(entityType, entityId)) {
            if (!entry.fieldName().equals(fieldName)) {
                continue;
            }
            if (!entry.tenantId().equals(command.tenantId())) {
                continue;
            }
            if (!entry.hasTranslatedValue()) {
                continue;
            }
            entriesByLocale.putIfAbsent(entry.locale(), entry);
        }

        List<String> localeChain = buildLocaleChain(requestedLocale, command.fallbackLocale());
        for (int index = 0; index < localeChain.size(); index++) {
            TranslationEntry resolved = entriesByLocale.get(localeChain.get(index));
            if (resolved != null) {
                boolean fallbackApplied = index > 0;
                return new TranslationResolutionView(
                        resolved.id(),
                        entityType,
                        entityId,
                        fieldName,
                        requestedLocale,
                        resolved.locale(),
                        resolved.translatedValue(),
                        resolved.translationStatus(),
                        fallbackApplied ? TranslationResolveSource.FALLBACK : TranslationResolveSource.EXACT,
                        fallbackApplied,
                        resolved.tenantId()
                );
            }
        }

        String originalValue = normalizeNullableText(command.originalValue());
        if (originalValue != null) {
            return new TranslationResolutionView(
                    null,
                    entityType,
                    entityId,
                    fieldName,
                    requestedLocale,
                    null,
                    originalValue,
                    null,
                    TranslationResolveSource.ORIGINAL_VALUE,
                    true,
                    command.tenantId()
            );
        }

        return new TranslationResolutionView(
                null,
                entityType,
                entityId,
                fieldName,
                requestedLocale,
                null,
                null,
                null,
                TranslationResolveSource.MISSING,
                false,
                command.tenantId()
        );
    }

    public List<TranslationEntryView> batchSaveTranslations(
            List<TranslationEntryCommands.BatchSaveItemCommand> entries
    ) {
        Objects.requireNonNull(entries, "entries must not be null");
        validateBatch(entries);
        Instant now = now();
        List<TranslationEntry> aggregates = new ArrayList<>(entries.size());
        try {
            for (TranslationEntryCommands.BatchSaveItemCommand entry : entries) {
                aggregates.add(toBatchAggregate(entry, now));
            }
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage(), ex);
        }
        return repository.batchSave(aggregates).stream().map(TranslationEntry::toView).toList();
    }

    public List<TranslationEntryView> queryByEntity(String entityType, String entityId) {
        return repository.findTranslationsByEntity(
                        requireText(entityType, "entityType"),
                        requireText(entityId, "entityId")
                )
                .stream()
                .map(TranslationEntry::toView)
                .toList();
    }

    private TranslationEntry toBatchAggregate(
            TranslationEntryCommands.BatchSaveItemCommand command,
            Instant updatedAt
    ) {
        Objects.requireNonNull(command, "batch entry must not be null");
        String entityType = requireText(command.entityType(), "entityType");
        String entityId = requireText(command.entityId(), "entityId");
        String fieldName = requireText(command.fieldName(), "fieldName");
        String locale = normalizeLocale(command.locale());
        Objects.requireNonNull(command.tenantId(), "tenantId must not be null");

        if (command.entryId() != null) {
            TranslationEntry existing = loadEntry(command.entryId());
            ensureIdentityMatches(existing, entityType, entityId, fieldName, locale, command.tenantId());
            return existing.translate(command.value(), command.updatedBy(), updatedAt);
        }

        return repository.findTranslation(entityType, entityId, fieldName, locale)
                .map(existing -> {
                    if (!existing.tenantId().equals(command.tenantId())) {
                        throw new BizException(
                                SharedErrorDescriptors.CONFLICT,
                                "translation entry tenant conflict for key: "
                                        + uniqueKey(entityType, entityId, fieldName, locale)
                        );
                    }
                    return existing.translate(command.value(), command.updatedBy(), updatedAt);
                })
                .orElseGet(() -> TranslationEntry.create(
                        entityType,
                        entityId,
                        fieldName,
                        locale,
                        command.value(),
                        command.tenantId(),
                        command.updatedBy(),
                        updatedAt
                ));
    }

    private void validateBatch(List<TranslationEntryCommands.BatchSaveItemCommand> entries) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            TranslationEntryCommands.BatchSaveItemCommand command = Objects.requireNonNull(
                    entries.get(index),
                    "batch entry must not be null"
            );
            String key = uniqueKey(
                    requireText(command.entityType(), "entityType"),
                    requireText(command.entityId(), "entityId"),
                    requireText(command.fieldName(), "fieldName"),
                    normalizeLocale(command.locale())
            );
            if (!uniqueKeys.add(key)) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "duplicate translation key in batch at index "
                                + index
                                + ": "
                                + key
                );
            }
        }
    }

    private void ensureIdentityMatches(
            TranslationEntry existing,
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId
    ) {
        if (!existing.entityType().equals(entityType)
                || !existing.entityId().equals(entityId)
                || !existing.fieldName().equals(fieldName)
                || !existing.locale().equals(locale)
                || !existing.tenantId().equals(tenantId)) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "batch save cannot change translation business identity for entryId: " + existing.id()
            );
        }
    }

    private TranslationEntry loadEntry(UUID entryId) {
        Objects.requireNonNull(entryId, "entryId must not be null");
        return repository.findById(entryId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "translation entry not found: " + entryId
                ));
    }

    private List<String> buildLocaleChain(String requestedLocale, String fallbackLocale) {
        LinkedHashSet<String> locales = new LinkedHashSet<>();
        locales.add(normalizeLocale(requestedLocale));
        if (fallbackLocale != null && !fallbackLocale.isBlank()) {
            locales.add(normalizeLocale(fallbackLocale));
        }
        for (String locale : DEFAULT_FALLBACK_LOCALES) {
            locales.add(normalizeLocale(locale));
        }
        return List.copyOf(locales);
    }

    private Instant now() {
        return clock.instant();
    }

    private String uniqueKey(
            String entityType,
            String entityId,
            String fieldName,
            String locale
    ) {
        return entityType + "|" + entityId + "|" + fieldName + "|" + locale;
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeLocale(String value) {
        String normalized = requireText(value, "locale").replace('_', '-');
        String[] segments = normalized.split("-");
        if (segments.length == 1) {
            return segments[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder builder = new StringBuilder(segments[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < segments.length; index++) {
            builder.append('-');
            builder.append(index == 1
                    ? segments[index].toUpperCase(Locale.ROOT)
                    : segments[index]);
        }
        return builder.toString();
    }
}
