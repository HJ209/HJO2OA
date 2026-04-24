package com.hjo2oa.infra.i18n.application;

import com.hjo2oa.infra.i18n.domain.LocaleBundle;
import com.hjo2oa.infra.i18n.domain.LocaleBundleRepository;
import com.hjo2oa.infra.i18n.domain.LocaleBundleView;
import com.hjo2oa.infra.i18n.domain.LocaleResourceEntry;
import com.hjo2oa.infra.i18n.domain.ResolvedLocaleMessageView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LocaleBundleApplicationService {

    private static final Comparator<LocaleBundle> BUNDLE_ORDER = Comparator
            .comparing(LocaleBundle::bundleCode)
            .thenComparing(LocaleBundle::locale)
            .thenComparing(bundle -> bundle.tenantId() == null ? "" : bundle.tenantId().toString());

    private final LocaleBundleRepository repository;
    private final Clock clock;

    public LocaleBundleApplicationService(LocaleBundleRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public LocaleBundleApplicationService(LocaleBundleRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public LocaleBundleView createBundle(LocaleBundleCommands.CreateBundleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        List<LocaleBundle> existing = repository.findByCode(command.bundleCode());
        String normalizedLocale = normalizeLocale(command.locale());
        ensureBundleUniqueness(existing, command.bundleCode(), normalizedLocale, command.tenantId());
        ensureNoFallbackCycle(existing, normalizedLocale, command.fallbackLocale(), command.tenantId());
        LocaleBundle bundle = LocaleBundle.create(
                UUID.randomUUID(),
                command.bundleCode(),
                command.moduleCode(),
                normalizedLocale,
                command.fallbackLocale(),
                command.tenantId(),
                now
        );
        return repository.save(bundle).toView();
    }

    public LocaleBundleView activateBundle(UUID bundleId) {
        LocaleBundle bundle = loadRequiredBundle(bundleId);
        return repository.save(bundle.activate(now())).toView();
    }

    public LocaleBundleView deprecateBundle(UUID bundleId) {
        LocaleBundle bundle = loadRequiredBundle(bundleId);
        return repository.save(bundle.deprecate(now())).toView();
    }

    public LocaleBundleView addEntry(UUID bundleId, String resourceKey, String resourceValue) {
        LocaleBundle bundle = loadRequiredBundle(bundleId);
        return repository.save(bundle.addEntry(resourceKey, resourceValue, now())).toView();
    }

    public LocaleBundleView addEntry(LocaleBundleCommands.BundleEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return addEntry(command.bundleId(), command.resourceKey(), command.resourceValue());
    }

    public LocaleBundleView updateEntry(UUID bundleId, String resourceKey, String resourceValue) {
        LocaleBundle bundle = loadRequiredBundle(bundleId);
        return repository.save(bundle.updateEntry(resourceKey, resourceValue, now())).toView();
    }

    public LocaleBundleView updateEntry(LocaleBundleCommands.BundleEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return updateEntry(command.bundleId(), command.resourceKey(), command.resourceValue());
    }

    public LocaleBundleView removeEntry(UUID bundleId, String resourceKey) {
        LocaleBundle bundle = loadRequiredBundle(bundleId);
        return repository.save(bundle.removeEntry(resourceKey, now())).toView();
    }

    public LocaleBundleView removeEntry(LocaleBundleCommands.RemoveBundleEntryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return removeEntry(command.bundleId(), command.resourceKey());
    }

    public ResolvedLocaleMessageView resolveMessage(
            String bundleCode,
            String resourceKey,
            String locale,
            UUID tenantId
    ) {
        List<LocaleBundle> bundles = repository.findByCode(bundleCode).stream()
                .filter(bundle -> bundle.status() == com.hjo2oa.infra.i18n.domain.LocaleBundleStatus.ACTIVE)
                .sorted(BUNDLE_ORDER)
                .toList();
        if (bundles.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Locale bundle not found");
        }

        String requestedLocale = normalizeLocale(locale);
        Set<String> visitedLocales = new LinkedHashSet<>();
        String currentLocale = requestedLocale;
        while (currentLocale != null && visitedLocales.add(currentLocale)) {
            LocaleBundle bundle = selectBundleForLocale(bundles, currentLocale, tenantId).orElse(null);
            if (bundle == null) {
                break;
            }
            Optional<LocaleResourceEntry> entry = bundle.findActiveEntry(resourceKey);
            if (entry.isPresent()) {
                return new ResolvedLocaleMessageView(
                        bundle.bundleCode(),
                        entry.get().resourceKey(),
                        requestedLocale,
                        bundle.locale(),
                        entry.get().resourceValue(),
                        bundle.tenantId(),
                        !bundle.locale().equals(requestedLocale)
                );
            }
            currentLocale = bundle.fallbackLocale();
        }
        throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Locale message not found");
    }

    public ResolvedLocaleMessageView resolveMessage(LocaleBundleCommands.ResolveMessageQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return resolveMessage(query.bundleCode(), query.resourceKey(), query.locale(), query.tenantId());
    }

    public List<LocaleBundleView> queryByCode(String bundleCode) {
        return repository.findByCode(bundleCode).stream()
                .sorted(BUNDLE_ORDER)
                .map(LocaleBundle::toView)
                .toList();
    }

    public List<LocaleBundleView> queryByModule(String moduleCode, String locale) {
        return repository.findByModuleAndLocale(moduleCode, normalizeLocale(locale)).stream()
                .sorted(BUNDLE_ORDER)
                .map(LocaleBundle::toView)
                .toList();
    }

    private LocaleBundle loadRequiredBundle(UUID bundleId) {
        return repository.findById(bundleId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Locale bundle not found"));
    }

    private void ensureBundleUniqueness(
            List<LocaleBundle> existing,
            String bundleCode,
            String locale,
            UUID tenantId
    ) {
        boolean duplicated = existing.stream()
                .anyMatch(bundle -> bundle.bundleCode().equals(bundleCode)
                        && bundle.locale().equals(locale)
                        && Objects.equals(bundle.tenantId(), tenantId));
        if (duplicated) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Locale bundle already exists");
        }
    }

    private void ensureNoFallbackCycle(
            List<LocaleBundle> existing,
            String locale,
            String fallbackLocale,
            UUID tenantId
    ) {
        String normalizedFallback = normalizeLocaleNullable(fallbackLocale);
        if (normalizedFallback == null) {
            return;
        }
        List<LocaleBundle> candidates = new ArrayList<>(existing);
        candidates.add(LocaleBundle.create(
                UUID.randomUUID(),
                existing.isEmpty() ? "TEMP" : existing.get(0).bundleCode(),
                existing.isEmpty() ? "TEMP" : existing.get(0).moduleCode(),
                locale,
                normalizedFallback,
                tenantId,
                now()
        ));
        Set<String> visited = new LinkedHashSet<>();
        String currentLocale = locale;
        while (currentLocale != null && visited.add(currentLocale)) {
            LocaleBundle bundle = selectBundleForLocale(candidates, currentLocale, tenantId).orElse(null);
            if (bundle == null) {
                return;
            }
            currentLocale = bundle.fallbackLocale();
        }
        if (currentLocale != null) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Locale fallback chain contains cycle");
        }
    }

    private Optional<LocaleBundle> selectBundleForLocale(List<LocaleBundle> bundles, String locale, UUID tenantId) {
        String normalizedLocale = normalizeLocale(locale);
        Optional<LocaleBundle> exact = bundles.stream()
                .filter(bundle -> bundle.locale().equals(normalizedLocale))
                .filter(bundle -> tenantMatches(bundle.tenantId(), tenantId))
                .min(bundleTenantComparator(tenantId));
        if (exact.isPresent()) {
            return exact;
        }
        int separatorIndex = normalizedLocale.indexOf('-');
        if (separatorIndex > 0) {
            String parentLocale = normalizedLocale.substring(0, separatorIndex);
            return bundles.stream()
                    .filter(bundle -> bundle.locale().equals(parentLocale))
                    .filter(bundle -> tenantMatches(bundle.tenantId(), tenantId))
                    .min(bundleTenantComparator(tenantId));
        }
        return Optional.empty();
    }

    private Comparator<LocaleBundle> bundleTenantComparator(UUID tenantId) {
        return Comparator.comparing(bundle -> tenantPreferred(bundle.tenantId(), tenantId) ? 0 : 1);
    }

    private boolean tenantMatches(UUID bundleTenantId, UUID tenantId) {
        if (tenantId == null) {
            return bundleTenantId == null;
        }
        return bundleTenantId == null || tenantId.equals(bundleTenantId);
    }

    private boolean tenantPreferred(UUID bundleTenantId, UUID tenantId) {
        return tenantId != null && tenantId.equals(bundleTenantId);
    }

    private Instant now() {
        return clock.instant();
    }

    private String normalizeLocale(String locale) {
        Objects.requireNonNull(locale, "locale must not be null");
        String normalized = locale.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("locale must not be blank");
        }
        return normalized.replace('_', '-').toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeLocaleNullable(String locale) {
        if (locale == null) {
            return null;
        }
        String normalized = locale.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalizeLocale(normalized);
    }
}
