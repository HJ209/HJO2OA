package com.hjo2oa.infra.config.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigEntryRepository {

    Optional<ConfigEntry> findById(UUID id);

    Optional<ConfigEntry> findByKey(String configKey);

    List<ConfigEntry> findAll();

    ConfigEntry save(ConfigEntry configEntry);
}
