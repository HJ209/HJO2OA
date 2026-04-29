package com.hjo2oa.portal.aggregation.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MybatisPortalCardSnapshotRepositoryTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeCachedIdentitySnapshotToDomainType() throws Exception {
        PortalIdentityCard identity = new PortalIdentityCard(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "position-1",
                "organization-1",
                "department-1",
                "Chief Clerk",
                "Head Office",
                "General Office",
                "PRIMARY",
                FIXED_TIME
        );
        MybatisPortalCardSnapshotRepository repository = repositoryReturning(
                PortalCardType.IDENTITY,
                objectMapper.writeValueAsString(identity)
        );

        Object data = repository.findByKey(key(PortalCardType.IDENTITY)).orElseThrow().data();

        assertThat(data).isInstanceOf(PortalIdentityCard.class);
        assertThat(((PortalIdentityCard) data).personId()).isEqualTo("person-1");
    }

    @Test
    void shouldDeserializeCachedTodoSnapshotToDomainType() throws Exception {
        PortalTodoCard todoCard = new PortalTodoCard(
                1,
                1,
                Map.of("approval", 1L),
                List.of(new PortalTodoItem(
                        "todo-1",
                        "Approve travel request",
                        "approval",
                        "HIGH",
                        FIXED_TIME.plusSeconds(600),
                        FIXED_TIME.minusSeconds(60)
                ))
        );
        MybatisPortalCardSnapshotRepository repository = repositoryReturning(
                PortalCardType.TODO,
                objectMapper.writeValueAsString(todoCard)
        );

        Object data = repository.findByKey(key(PortalCardType.TODO)).orElseThrow().data();

        assertThat(data).isInstanceOf(PortalTodoCard.class);
        assertThat(((PortalTodoCard) data).topItems()).hasSize(1);
    }

    @Test
    void shouldDeserializeCachedMessageSnapshotToDomainType() throws Exception {
        PortalMessageCard messageCard = new PortalMessageCard(
                1,
                Map.of("TODO_CREATED", 1L),
                List.of(new PortalMessageItem(
                        "notification-1",
                        "Approve travel request",
                        "TODO_CREATED",
                        "HIGH",
                        "/portal/todo/todo-1",
                        FIXED_TIME.minusSeconds(30)
                ))
        );
        MybatisPortalCardSnapshotRepository repository = repositoryReturning(
                PortalCardType.MESSAGE,
                objectMapper.writeValueAsString(messageCard)
        );

        Object data = repository.findByKey(key(PortalCardType.MESSAGE)).orElseThrow().data();

        assertThat(data).isInstanceOf(PortalMessageCard.class);
        assertThat(((PortalMessageCard) data).topItems()).hasSize(1);
    }

    private MybatisPortalCardSnapshotRepository repositoryReturning(PortalCardType cardType, String dataJson) {
        PortalCardSnapshotMapper mapper = mock(PortalCardSnapshotMapper.class);
        PortalAggregationSnapshotKey key = key(cardType);
        when(mapper.selectById(key.asCacheKey())).thenReturn(entity(key, dataJson));
        return new MybatisPortalCardSnapshotRepository(mapper, objectMapper);
    }

    private PortalCardSnapshotEntity entity(PortalAggregationSnapshotKey key, String dataJson) {
        PortalCardSnapshotEntity entity = new PortalCardSnapshotEntity();
        entity.setSnapshotId(key.asCacheKey());
        entity.setTenantId(key.tenantId());
        entity.setPersonId(key.personId());
        entity.setAssignmentId(key.assignmentId());
        entity.setPositionId(key.positionId());
        entity.setSceneType(key.sceneType().name());
        entity.setCardType(key.cardType().name());
        entity.setState(PortalCardState.READY.name());
        entity.setDataJson(dataJson);
        entity.setRefreshedAt(FIXED_TIME);
        return entity;
    }

    private PortalAggregationSnapshotKey key(PortalCardType cardType) {
        return new PortalAggregationSnapshotKey(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1",
                PortalSceneType.HOME,
                cardType
        );
    }
}
