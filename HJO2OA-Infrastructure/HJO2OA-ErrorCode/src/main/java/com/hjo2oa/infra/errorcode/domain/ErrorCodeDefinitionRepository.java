package com.hjo2oa.infra.errorcode.domain;

import java.util.List;
import java.util.Optional;

public interface ErrorCodeDefinitionRepository {

    Optional<ErrorCodeDefinition> findByCode(String code);

    List<ErrorCodeDefinition> findByModule(String moduleCode);

    ErrorCodeDefinition save(ErrorCodeDefinition definition);

    List<ErrorCodeDefinition> findAll();
}
