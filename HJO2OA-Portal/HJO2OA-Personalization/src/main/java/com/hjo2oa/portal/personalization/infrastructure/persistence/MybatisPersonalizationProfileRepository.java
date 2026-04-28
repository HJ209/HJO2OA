package com.hjo2oa.portal.personalization.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileKey;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileStatus;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPersonalizationProfileRepository implements PersonalizationProfileRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<QuickAccessEntry>> QUICK_ACCESS_LIST = new TypeReference<>() {
    };

    private final PersonalizationProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisPersonalizationProfileRepository(PersonalizationProfileMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PersonalizationProfile> findByKey(PersonalizationProfileKey key) {
        QueryWrapper<PersonalizationProfileEntity> wrapper = new QueryWrapper<PersonalizationProfileEntity>()
                .eq("tenant_id", key.tenantId())
                .eq("person_id", key.personId())
                .eq("scene_type", key.sceneType().name());
        if (key.assignmentId() == null) {
            wrapper.isNull("assignment_id");
        } else {
            wrapper.eq("assignment_id", key.assignmentId());
        }
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public PersonalizationProfile save(PersonalizationProfile profile) {
        PersonalizationProfileEntity existing = mapper.selectById(profile.profileId());
        PersonalizationProfileEntity entity = toEntity(profile, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByKey(profile.key()).orElseThrow();
    }

    private PersonalizationProfile toDomain(PersonalizationProfileEntity entity) {
        return new PersonalizationProfile(
                entity.getProfileId(),
                entity.getTenantId(),
                entity.getPersonId(),
                entity.getAssignmentId(),
                PersonalizationSceneType.valueOf(entity.getSceneType()),
                entity.getBasePublicationId(),
                entity.getThemeCode(),
                read(entity.getWidgetOrderJson(), STRING_LIST),
                read(entity.getHiddenPlacementJson(), STRING_LIST),
                read(entity.getQuickAccessJson(), QUICK_ACCESS_LIST),
                PersonalizationProfileStatus.valueOf(entity.getStatus()),
                entity.getLastResolvedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PersonalizationProfileEntity toEntity(PersonalizationProfile profile, PersonalizationProfileEntity existing) {
        PersonalizationProfileEntity entity = existing == null ? new PersonalizationProfileEntity() : existing;
        entity.setProfileId(profile.profileId());
        entity.setTenantId(profile.tenantId());
        entity.setPersonId(profile.personId());
        entity.setAssignmentId(profile.assignmentId());
        entity.setSceneType(profile.sceneType().name());
        entity.setBasePublicationId(profile.basePublicationId());
        entity.setThemeCode(profile.themeCode());
        entity.setWidgetOrderJson(write(profile.widgetOrderOverride()));
        entity.setHiddenPlacementJson(write(profile.hiddenPlacementCodes()));
        entity.setQuickAccessJson(write(profile.quickAccessEntries()));
        entity.setStatus(profile.status().name());
        entity.setLastResolvedAt(profile.lastResolvedAt());
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return json == null || json.isBlank() ? objectMapper.readValue("[]", type) : objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid personalization JSON", ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write personalization JSON", ex);
        }
    }
}
