package com.hjo2oa.infra.dictionary.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DictionaryTypeRepository {

    Optional<DictionaryType> findById(UUID typeId);

    Optional<DictionaryType> findByCode(UUID tenantId, String code);

    List<DictionaryType> findByTenant(UUID tenantId);

    DictionaryType save(DictionaryType type);

    List<DictionaryType> findAllActive();
}
