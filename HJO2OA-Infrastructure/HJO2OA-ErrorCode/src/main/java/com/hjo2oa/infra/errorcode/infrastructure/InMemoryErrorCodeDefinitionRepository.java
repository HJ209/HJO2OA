package com.hjo2oa.infra.errorcode.infrastructure;

import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinition;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryErrorCodeDefinitionRepository implements ErrorCodeDefinitionRepository {

    private final Map<String, ErrorCodeDefinition> definitionsByCode = new LinkedHashMap<>();

    @Override
    public Optional<ErrorCodeDefinition> findByCode(String code) {
        return Optional.ofNullable(definitionsByCode.get(code));
    }

    @Override
    public List<ErrorCodeDefinition> findByModule(String moduleCode) {
        return definitionsByCode.values().stream()
                .filter(definition -> definition.moduleCode().equals(moduleCode))
                .sorted(Comparator.comparing(ErrorCodeDefinition::code))
                .toList();
    }

    @Override
    public ErrorCodeDefinition save(ErrorCodeDefinition definition) {
        definitionsByCode.put(definition.code(), definition);
        return definition;
    }

    @Override
    public List<ErrorCodeDefinition> findAll() {
        return definitionsByCode.values().stream()
                .sorted(Comparator.comparing(ErrorCodeDefinition::moduleCode).thenComparing(ErrorCodeDefinition::code))
                .toList();
    }
}
