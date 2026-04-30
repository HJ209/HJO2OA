package com.hjo2oa.infra.i18n.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocaleBundleRepository {

    Optional<LocaleBundle> findById(UUID bundleId);

    List<LocaleBundle> findAll();

    List<LocaleBundle> findByCode(String bundleCode);

    List<LocaleBundle> findByModuleAndLocale(String moduleCode, String locale);

    List<LocaleBundle> findByLocale(String locale, UUID tenantId);

    List<LocaleBundle> findActiveByLocale(String locale, UUID tenantId);

    LocaleBundle save(LocaleBundle bundle);
}
