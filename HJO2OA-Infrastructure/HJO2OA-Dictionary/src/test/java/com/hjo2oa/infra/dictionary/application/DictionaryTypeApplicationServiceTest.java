package com.hjo2oa.infra.dictionary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeUpdatedEvent;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.infra.dictionary.infrastructure.InMemoryDictionaryTypeRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DictionaryTypeApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldCreateDictionaryTypeAndPublishUpdatedEvent() {
        InMemoryDictionaryTypeRepository repository = new InMemoryDictionaryTypeRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        DictionaryTypeApplicationService applicationService = applicationService(repository, publishedEvents);

        DictionaryTypeView dictionaryType = applicationService.createType(
                "leave-type",
                "Leave Type",
                "workflow",
                false,
                true,
                TENANT_ID
        );

        assertThat(dictionaryType.code()).isEqualTo("leave-type");
        assertThat(dictionaryType.status()).isEqualTo(DictionaryStatus.ACTIVE);
        assertThat(publishedEvents).singleElement().isInstanceOf(DictionaryTypeUpdatedEvent.class);
        DictionaryTypeUpdatedEvent event = (DictionaryTypeUpdatedEvent) publishedEvents.get(0);
        assertThat(event.eventType()).isEqualTo(DictionaryTypeUpdatedEvent.EVENT_TYPE);
        assertThat(event.dictionaryCode()).isEqualTo("leave-type");
        assertThat(event.tenantId()).isEqualTo(TENANT_ID.toString());
    }

    @Test
    void shouldManageDictionaryItemsAndPublishEvents() {
        InMemoryDictionaryTypeRepository repository = new InMemoryDictionaryTypeRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        DictionaryTypeApplicationService applicationService = applicationService(repository, publishedEvents);
        DictionaryTypeView dictionaryType = applicationService.createType(
                "priority",
                "Priority",
                "task",
                true,
                true,
                TENANT_ID
        );
        publishedEvents.clear();

        DictionaryTypeView withParent = applicationService.addItem(
                dictionaryType.id(),
                "P1",
                "High",
                null,
                10
        );
        UUID parentItemId = withParent.items().get(0).id();

        DictionaryTypeView withChild = applicationService.addItem(
                dictionaryType.id(),
                "P1-1",
                "Top Priority",
                parentItemId,
                20
        );
        UUID childItemId = withChild.items().stream()
                .filter(item -> item.parentItemId() != null)
                .findFirst()
                .orElseThrow()
                .id();

        DictionaryTypeView updatedChild = applicationService.updateItem(
                dictionaryType.id(),
                childItemId,
                "Critical",
                30
        );
        assertThat(updatedChild.items()).anySatisfy(item -> {
            if (item.id().equals(childItemId)) {
                assertThat(item.displayName()).isEqualTo("Critical");
                assertThat(item.sortOrder()).isEqualTo(30);
            }
        });

        DictionaryTypeView disabledChild = applicationService.disableItem(dictionaryType.id(), childItemId);
        assertThat(disabledChild.items()).anySatisfy(item -> {
            if (item.id().equals(childItemId)) {
                assertThat(item.enabled()).isFalse();
            }
        });

        DictionaryTypeView enabledChild = applicationService.enableItem(dictionaryType.id(), childItemId);
        assertThat(enabledChild.items()).anySatisfy(item -> {
            if (item.id().equals(childItemId)) {
                assertThat(item.enabled()).isTrue();
            }
        });

        DictionaryTypeView afterChildRemoval = applicationService.removeItem(dictionaryType.id(), childItemId);
        assertThat(afterChildRemoval.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(parentItemId));
        DictionaryTypeView afterParentRemoval = applicationService.removeItem(dictionaryType.id(), parentItemId);
        assertThat(afterParentRemoval.items()).isEmpty();
        assertThat(publishedEvents).hasSize(7);
        assertThat(publishedEvents).allSatisfy(event -> assertThat(event.eventType())
                .isEqualTo(DictionaryTypeUpdatedEvent.EVENT_TYPE));
    }

    @Test
    void shouldDisableAndEnableDictionaryType() {
        DictionaryTypeApplicationService applicationService = applicationService(
                new InMemoryDictionaryTypeRepository(),
                new ArrayList<>()
        );
        DictionaryTypeView dictionaryType = applicationService.createType(
                "notice-level",
                "Notice Level",
                "portal",
                false,
                false,
                TENANT_ID
        );

        DictionaryTypeView disabledType = applicationService.disableType(dictionaryType.id());
        DictionaryTypeView enabledType = applicationService.enableType(dictionaryType.id());

        assertThat(disabledType.status()).isEqualTo(DictionaryStatus.DISABLED);
        assertThat(enabledType.status()).isEqualTo(DictionaryStatus.ACTIVE);
    }

    @Test
    void shouldRejectDuplicateDictionaryCodeWithinSameTenant() {
        DictionaryTypeApplicationService applicationService = applicationService(
                new InMemoryDictionaryTypeRepository(),
                new ArrayList<>()
        );
        applicationService.createType("leave-type", "Leave Type", "workflow", false, true, TENANT_ID);

        assertThatThrownBy(() -> applicationService.createType(
                "leave-type",
                "Leave Type Copy",
                "workflow",
                false,
                false,
                TENANT_ID
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Dictionary code already exists");
    }

    @Test
    void shouldRejectInvalidHierarchyOperations() {
        DictionaryTypeApplicationService applicationService = applicationService(
                new InMemoryDictionaryTypeRepository(),
                new ArrayList<>()
        );
        DictionaryTypeView flatType = applicationService.createType(
                "gender",
                "Gender",
                "hr",
                false,
                true,
                TENANT_ID
        );

        assertThatThrownBy(() -> applicationService.addItem(
                flatType.id(),
                "male",
                "Male",
                UUID.randomUUID(),
                1
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Parent item is only allowed for hierarchical dictionaries");
    }

    @Test
    void shouldQueryByCodeAndListActiveDictionaries() {
        DictionaryTypeApplicationService applicationService = applicationService(
                new InMemoryDictionaryTypeRepository(),
                new ArrayList<>()
        );
        DictionaryTypeView globalType = applicationService.createType(
                "country",
                "Country",
                "common",
                false,
                true,
                null
        );
        DictionaryTypeView tenantType = applicationService.createType(
                "priority",
                "Priority",
                "task",
                false,
                true,
                TENANT_ID
        );
        applicationService.disableType(tenantType.id());

        assertThat(applicationService.queryByCode(null, "country"))
                .get()
                .extracting(DictionaryTypeView::id)
                .isEqualTo(globalType.id());
        assertThat(applicationService.listTypes(null))
                .extracting(DictionaryTypeView::code)
                .containsExactly("country");
        assertThat(applicationService.listTypes(TENANT_ID))
                .extracting(DictionaryTypeView::code)
                .containsExactly("priority");
    }

    private DictionaryTypeApplicationService applicationService(
            InMemoryDictionaryTypeRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        return new DictionaryTypeApplicationService(
                repository,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
