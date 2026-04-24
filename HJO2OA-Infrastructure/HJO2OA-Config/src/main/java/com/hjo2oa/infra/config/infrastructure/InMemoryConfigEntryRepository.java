package com.hjo2oa.infra.config.infrastructure;

import com.hjo2oa.infra.config.domain.ConfigEntry;
import com.hjo2oa.infra.config.domain.ConfigEntryRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryConfigEntryRepository implements ConfigEntryRepository {

    private final Map<UUID, ConfigEntry> entriesById = new ConcurrentHashMap<>();

    @Override
    public Optional<ConfigEntry> findById(UUID id) {
        return Optional.ofNullable(entriesById.get(id));
    }

    @Override
    public Optional<ConfigEntry> findByKey(String configKey) {
        return entriesById.values().stream()
                .filter(entry -> entry.configKey().equals(configKey))
                .findFirst();
    }

    @Override
    public List<ConfigEntry> findAll() {
        return entriesById.values().stream()
                .sorted(Comparator.comparing(ConfigEntry::updatedAt).reversed().thenComparing(ConfigEntry::configKey))
                .toList();
    }

    @Override
    public ConfigEntry save(ConfigEntry configEntry) {
        entriesById.put(configEntry.id(), configEntry);
        return configEntry;
    }
}
