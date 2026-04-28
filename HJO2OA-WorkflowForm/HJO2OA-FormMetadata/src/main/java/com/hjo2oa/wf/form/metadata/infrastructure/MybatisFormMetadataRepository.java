package com.hjo2oa.wf.form.metadata.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadata;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataRepository;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
public class MybatisFormMetadataRepository implements FormMetadataRepository {

    private static final TypeReference<List<FormFieldDefinition>> FIELD_SCHEMA_TYPE =
            new TypeReference<>() {
            };

    private final FormMetadataMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisFormMetadataRepository(FormMetadataMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public Optional<FormMetadata> findById(UUID metadataId) {
        return Optional.ofNullable(mapper.selectById(metadataId)).map(this::toDomain);
    }

    @Override
    public Optional<FormMetadata> findByCodeAndVersion(UUID tenantId, String code, int version) {
        LambdaQueryWrapper<FormMetadataEntity> wrapper = Wrappers.<FormMetadataEntity>lambdaQuery()
                .eq(FormMetadataEntity::getTenantId, tenantId)
                .eq(FormMetadataEntity::getCode, normalizeCode(code))
                .eq(FormMetadataEntity::getVersion, version);
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Optional<FormMetadata> findLatestPublished(UUID tenantId, String code) {
        LambdaQueryWrapper<FormMetadataEntity> wrapper = Wrappers.<FormMetadataEntity>lambdaQuery()
                .eq(FormMetadataEntity::getTenantId, tenantId)
                .eq(FormMetadataEntity::getCode, normalizeCode(code))
                .eq(FormMetadataEntity::getStatus, FormMetadataStatus.PUBLISHED.name())
                .orderByDesc(FormMetadataEntity::getVersion);
        return mapper.selectList(wrapper).stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<FormMetadata> findByCode(UUID tenantId, String code) {
        LambdaQueryWrapper<FormMetadataEntity> wrapper = Wrappers.<FormMetadataEntity>lambdaQuery()
                .eq(FormMetadataEntity::getTenantId, tenantId)
                .eq(FormMetadataEntity::getCode, normalizeCode(code))
                .orderByDesc(FormMetadataEntity::getVersion);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<FormMetadata> findByTenant(UUID tenantId) {
        LambdaQueryWrapper<FormMetadataEntity> wrapper = Wrappers.<FormMetadataEntity>lambdaQuery()
                .eq(FormMetadataEntity::getTenantId, tenantId)
                .orderByDesc(FormMetadataEntity::getUpdatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public FormMetadata save(FormMetadata metadata) {
        FormMetadataEntity entity = toEntity(metadata);
        if (mapper.selectById(metadata.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return metadata;
    }

    private FormMetadataEntity toEntity(FormMetadata metadata) {
        return new FormMetadataEntity()
                .setId(metadata.id())
                .setCode(metadata.code())
                .setName(metadata.name())
                .setNameI18nKey(metadata.nameI18nKey())
                .setVersion(metadata.version())
                .setStatus(metadata.status().name())
                .setFieldSchema(writeJson(metadata.fieldSchema()))
                .setLayout(writeJson(metadata.layout()))
                .setValidations(writeJson(metadata.validations()))
                .setFieldPermissionMap(writeJson(metadata.fieldPermissionMap()))
                .setTenantId(metadata.tenantId())
                .setPublishedAt(metadata.publishedAt())
                .setCreatedAt(metadata.createdAt())
                .setUpdatedAt(metadata.updatedAt());
    }

    private FormMetadata toDomain(FormMetadataEntity entity) {
        return new FormMetadata(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getNameI18nKey(),
                entity.getVersion(),
                FormMetadataStatus.valueOf(entity.getStatus()),
                readFieldSchema(entity.getFieldSchema()),
                readJson(entity.getLayout()),
                readJson(entity.getValidations()),
                readJson(entity.getFieldPermissionMap()),
                entity.getTenantId(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<FormFieldDefinition> readFieldSchema(String value) {
        try {
            return objectMapper.readValue(value, FIELD_SCHEMA_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid form field_schema JSON", ex);
        }
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid form metadata JSON", ex);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write form metadata JSON", ex);
        }
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }
}
