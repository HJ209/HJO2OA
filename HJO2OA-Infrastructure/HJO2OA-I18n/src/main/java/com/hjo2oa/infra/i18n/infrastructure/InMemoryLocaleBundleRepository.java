package com.hjo2oa.infra.i18n.infrastructure;

import com.hjo2oa.infra.i18n.domain.LocaleBundle;
import com.hjo2oa.infra.i18n.domain.LocaleBundleRepository;
import com.hjo2oa.infra.i18n.domain.LocaleBundleStatus;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryLocaleBundleRepository implements LocaleBundleRepository {

    private static final Comparator<LocaleBundle> BUNDLE_ORDER = Comparator
            .comparing(LocaleBundle::bundleCode)
            .thenComparing(LocaleBundle::locale)
            .thenComparing(bundle -> bundle.tenantId() == null ? "" : bundle.tenantId().toString());

    private final Map<UUID, LocaleBundle> bundlesById = new LinkedHashMap<>();

    @Override
    public Optional<LocaleBundle> findById(UUID bundleId) {
        return Optional.ofNullable(bundlesById.get(bundleId));
    }

    @Override
    public List<LocaleBundle> findByCode(String bundleCode) {
        return bundlesById.values().stream()
                .filter(bundle -> bundle.bundleCode().equals(bundleCode))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public List<LocaleBundle> findByModuleAndLocale(String moduleCode, String locale) {
        return bundlesById.values().stream()
                .filter(bundle -> bundle.moduleCode().equals(moduleCode))
                .filter(bundle -> bundle.locale().equals(locale))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public List<LocaleBundle> findActiveByLocale(String locale, UUID tenantId) {
        return bundlesById.values().stream()
                .filter(bundle -> bundle.status() == LocaleBundleStatus.ACTIVE)
                .filter(bundle -> bundle.locale().equals(locale))
                .filter(bundle -> tenantMatches(bundle.tenantId(), tenantId))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public LocaleBundle save(LocaleBundle bundle) {
        bundlesById.put(bundle.id(), Objects.requireNonNull(bundle, "bundle must not be null"));
        return bundle;
    }

    private boolean tenantMatches(UUID bundleTenantId, UUID tenantId) {
        if (tenantId == null) {
            return bundleTenantId == null;
        }
        return bundleTenantId == null || tenantId.equals(bundleTenantId);
    }
}
