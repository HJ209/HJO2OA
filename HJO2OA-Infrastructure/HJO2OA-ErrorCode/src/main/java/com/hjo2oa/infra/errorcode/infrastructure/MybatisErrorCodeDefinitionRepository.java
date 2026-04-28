package com.hjo2oa.infra.errorcode.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinition;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionRepository;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisErrorCodeDefinitionRepository implements ErrorCodeDefinitionRepository {

    private final ErrorCodeDefinitionMapper mapper;

    public MybatisErrorCodeDefinitionRepository(ErrorCodeDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ErrorCodeDefinition> findByCode(String code) {
        LambdaQueryWrapper<ErrorCodeDefinitionEntity> query = new LambdaQueryWrapper<ErrorCodeDefinitionEntity>()
                .eq(ErrorCodeDefinitionEntity::getCode, code);
        return Optional.ofNullable(mapper.selectOne(query)).map(this::toDomain);
    }

    @Override
    public List<ErrorCodeDefinition> findByModule(String moduleCode) {
        LambdaQueryWrapper<ErrorCodeDefinitionEntity> query = new LambdaQueryWrapper<ErrorCodeDefinitionEntity>()
                .eq(ErrorCodeDefinitionEntity::getModuleCode, moduleCode)
                .orderByAsc(ErrorCodeDefinitionEntity::getCode);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public ErrorCodeDefinition save(ErrorCodeDefinition definition) {
        ErrorCodeDefinitionEntity entity = toEntity(definition);
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return definition;
    }

    @Override
    public List<ErrorCodeDefinition> findAll() {
        LambdaQueryWrapper<ErrorCodeDefinitionEntity> query = new LambdaQueryWrapper<ErrorCodeDefinitionEntity>()
                .orderByAsc(ErrorCodeDefinitionEntity::getModuleCode)
                .orderByAsc(ErrorCodeDefinitionEntity::getCode);
        return mapper.selectList(query).stream().map(this::toDomain).toList();
    }

    private ErrorCodeDefinition toDomain(ErrorCodeDefinitionEntity entity) {
        return new ErrorCodeDefinition(
                UUID.fromString(entity.getId()),
                entity.getCode(),
                entity.getModuleCode(),
                entity.getCategory(),
                ErrorSeverity.valueOf(entity.getSeverity()),
                entity.getHttpStatus(),
                entity.getMessageKey(),
                Boolean.TRUE.equals(entity.getRetryable()),
                Boolean.TRUE.equals(entity.getDeprecated()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ErrorCodeDefinitionEntity toEntity(ErrorCodeDefinition definition) {
        return new ErrorCodeDefinitionEntity()
                .setId(definition.id().toString())
                .setCode(definition.code())
                .setModuleCode(definition.moduleCode())
                .setCategory(definition.category())
                .setSeverity(definition.severity().name())
                .setHttpStatus(definition.httpStatus())
                .setMessageKey(definition.messageKey())
                .setRetryable(definition.retryable())
                .setDeprecated(definition.deprecated())
                .setCreatedAt(definition.createdAt())
                .setUpdatedAt(definition.updatedAt());
    }
}
