package com.hjo2oa.org.org.sync.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hjo2oa.org.org.sync.audit.domain.SourceStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MybatisSyncSourceConfigRepositoryTest {

    @Test
    void saveShouldInsertAndMapSourceConfig() {
        SyncSourceConfigMapper mapper = mock(SyncSourceConfigMapper.class);
        MybatisSyncSourceConfigRepository repository = new MybatisSyncSourceConfigRepository(mapper);
        SyncSourceConfig source = SyncSourceConfig.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ldap",
                "LDAP",
                "LDAP",
                null,
                "config/org/ldap",
                null,
                Instant.parse("2026-04-27T00:00:00Z")
        );
        when(mapper.selectById(source.id())).thenReturn(null, toEntity(source));

        SyncSourceConfig saved = repository.save(source);

        verify(mapper).insert(any(SyncSourceConfigEntity.class));
        assertThat(saved.id()).isEqualTo(source.id());
        assertThat(saved.status()).isEqualTo(SourceStatus.DISABLED);
        assertThat(saved.configRef()).isEqualTo("config/org/ldap");
    }

    @Test
    void findByTenantShouldMapEntityList() {
        SyncSourceConfigMapper mapper = mock(SyncSourceConfigMapper.class);
        MybatisSyncSourceConfigRepository repository = new MybatisSyncSourceConfigRepository(mapper);
        SyncSourceConfig source = SyncSourceConfig.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ding",
                "DingTalk",
                "DINGTALK",
                null,
                "config/org/ding",
                null,
                Instant.parse("2026-04-27T00:00:00Z")
        );
        when(mapper.selectList(any())).thenReturn(List.of(toEntity(source)));

        assertThat(repository.findByTenantId(source.tenantId()))
                .hasSize(1)
                .first()
                .extracting(SyncSourceConfig::sourceCode)
                .isEqualTo("ding");
    }

    private static SyncSourceConfigEntity toEntity(SyncSourceConfig source) {
        SyncSourceConfigEntity entity = new SyncSourceConfigEntity();
        entity.setId(source.id());
        entity.setTenantId(source.tenantId());
        entity.setSourceCode(source.sourceCode());
        entity.setSourceName(source.sourceName());
        entity.setSourceType(source.sourceType());
        entity.setEndpoint(source.endpoint());
        entity.setConfigRef(source.configRef());
        entity.setScopeExpression(source.scopeExpression());
        entity.setStatus(source.status().name());
        entity.setCreatedAt(source.createdAt());
        entity.setUpdatedAt(source.updatedAt());
        return entity;
    }
}
