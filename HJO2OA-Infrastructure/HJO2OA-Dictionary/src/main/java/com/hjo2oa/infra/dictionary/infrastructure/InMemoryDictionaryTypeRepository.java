package com.hjo2oa.infra.dictionary.infrastructure;

import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import com.hjo2oa.infra.dictionary.domain.DictionaryType;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeRepository;
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
public class InMemoryDictionaryTypeRepository implements DictionaryTypeRepository {

    private final Map<UUID, DictionaryType> dictionaryTypes = new ConcurrentHashMap<>();

    @Override
    public Optional<DictionaryType> findById(UUID typeId) {
        return Optional.ofNullable(dictionaryTypes.get(typeId));
    }

    @Override
    public Optional<DictionaryType> findByCode(UUID tenantId, String code) {
        return dictionaryTypes.values().stream()
                .filter(dictionaryType -> tenantEquals(dictionaryType.tenantId(), tenantId))
                .filter(dictionaryType -> dictionaryType.code().equals(code))
                .findFirst();
    }

    @Override
    public List<DictionaryType> findByTenant(UUID tenantId) {
        return dictionaryTypes.values().stream()
                .filter(dictionaryType -> tenantEquals(dictionaryType.tenantId(), tenantId))
                .sorted(Comparator.comparingInt(DictionaryType::sortOrder)
                        .thenComparing(DictionaryType::code)
                        .thenComparing(DictionaryType::id))
                .toList();
    }

    @Override
    public DictionaryType save(DictionaryType type) {
        dictionaryTypes.put(type.id(), type);
        return type;
    }

    @Override
    public List<DictionaryType> findAllActive() {
        return dictionaryTypes.values().stream()
                .filter(dictionaryType -> dictionaryType.status() == DictionaryStatus.ACTIVE)
                .sorted(Comparator.comparingInt(DictionaryType::sortOrder)
                        .thenComparing(DictionaryType::code)
                        .thenComparing(DictionaryType::id))
                .toList();
    }

    private boolean tenantEquals(UUID left, UUID right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
