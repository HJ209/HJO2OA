package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.cache.application.NoopCacheRuntimeService;
import com.hjo2oa.infra.dictionary.domain.DictionaryItemView;
import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import com.hjo2oa.infra.dictionary.domain.DictionaryType;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeRepository;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeUpdatedEvent;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DictionaryTypeApplicationService {

    private final DictionaryTypeRepository dictionaryTypeRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final DictionaryCacheService dictionaryCacheService;
    private final Clock clock;

    @Autowired
    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher,
            DictionaryCacheService dictionaryCacheService
    ) {
        this(dictionaryTypeRepository, domainEventPublisher, dictionaryCacheService, Clock.systemUTC());
    }

    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                dictionaryTypeRepository,
                domainEventPublisher,
                new DictionaryCacheService(new NoopCacheRuntimeService()),
                Clock.systemUTC()
        );
    }

    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(
                dictionaryTypeRepository,
                domainEventPublisher,
                new DictionaryCacheService(new NoopCacheRuntimeService()),
                clock
        );
    }

    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher,
            DictionaryCacheService dictionaryCacheService,
            Clock clock
    ) {
        this.dictionaryTypeRepository = Objects.requireNonNull(
                dictionaryTypeRepository,
                "dictionaryTypeRepository must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.dictionaryCacheService =
                Objects.requireNonNull(dictionaryCacheService, "dictionaryCacheService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DictionaryTypeView createType(DictionaryTypeCommands.CreateTypeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return createType(
                command.code(),
                command.name(),
                command.category(),
                command.hierarchical(),
                command.cacheable(),
                command.sortOrder(),
                command.tenantId()
        );
    }

    public DictionaryTypeView createType(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            UUID tenantId
    ) {
        return createType(code, name, category, hierarchical, cacheable, 0, tenantId);
    }

    public DictionaryTypeView createType(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            Integer sortOrder,
            UUID tenantId
    ) {
        return createType(code, name, category, hierarchical, cacheable, sortOrder, false, tenantId);
    }

    public DictionaryTypeView createSystemType(
            String code,
            String name,
            String category,
            Integer sortOrder,
            UUID tenantId
    ) {
        return createType(code, name, category, false, true, sortOrder, true, tenantId);
    }

    public DictionaryTypeView updateType(
            UUID typeId,
            UUID requestTenantId,
            DictionaryTypeCommands.UpdateTypeCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.update(
                command.name(),
                command.category(),
                command.hierarchical(),
                command.cacheable(),
                command.sortOrder(),
                now
        ));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView disableType(UUID typeId) {
        return disableType(typeId, null);
    }

    public DictionaryTypeView disableType(UUID typeId, UUID requestTenantId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType disabledType = dictionaryType.disable(now);
        if (disabledType == dictionaryType) {
            return dictionaryType.toView();
        }
        DictionaryType savedType = dictionaryTypeRepository.save(disabledType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView enableType(UUID typeId) {
        return enableType(typeId, null);
    }

    public DictionaryTypeView enableType(UUID typeId, UUID requestTenantId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType enabledType = dictionaryType.enable(now);
        if (enabledType == dictionaryType) {
            return dictionaryType.toView();
        }
        DictionaryType savedType = dictionaryTypeRepository.save(enabledType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView addItem(UUID typeId, DictionaryTypeCommands.AddItemCommand command) {
        return addItem(typeId, null, command);
    }

    public DictionaryTypeView addItem(UUID typeId, UUID requestTenantId, DictionaryTypeCommands.AddItemCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return addItem(
                typeId,
                requestTenantId,
                command.itemCode(),
                command.displayName(),
                command.parentItemId(),
                command.sortOrder(),
                Boolean.TRUE.equals(command.defaultItem()),
                command.multiLangValue(),
                command.extensionJson(),
                false
        );
    }

    public DictionaryTypeView addItem(
            UUID typeId,
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder
    ) {
        return addItem(typeId, null, itemCode, displayName, parentItemId, sortOrder, false, null, null, false);
    }

    public DictionaryTypeView updateItem(
            UUID typeId,
            UUID itemId,
            DictionaryTypeCommands.UpdateItemCommand command
    ) {
        return updateItem(typeId, itemId, null, command);
    }

    public DictionaryTypeView updateItem(
            UUID typeId,
            UUID itemId,
            UUID requestTenantId,
            DictionaryTypeCommands.UpdateItemCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.updateItem(
                itemId,
                command.displayName(),
                command.parentItemId(),
                command.sortOrder(),
                command.defaultItem(),
                command.multiLangValue(),
                command.extensionJson(),
                now
        ));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView updateItem(UUID typeId, UUID itemId, String displayName, Integer sortOrder) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.updateItem(itemId, displayName, sortOrder, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView removeItem(UUID typeId, UUID itemId) {
        return removeItem(typeId, itemId, null);
    }

    public DictionaryTypeView removeItem(UUID typeId, UUID itemId, UUID requestTenantId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.removeItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView enableItem(UUID typeId, UUID itemId) {
        return enableItem(typeId, itemId, null);
    }

    public DictionaryTypeView enableItem(UUID typeId, UUID itemId, UUID requestTenantId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.enableItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView disableItem(UUID typeId, UUID itemId) {
        return disableItem(typeId, itemId, null);
    }

    public DictionaryTypeView disableItem(UUID typeId, UUID itemId, UUID requestTenantId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        ensureMutable(dictionaryType);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.disableItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView upsertSystemItem(
            UUID typeId,
            String itemCode,
            String displayName,
            Integer sortOrder,
            boolean enabled
    ) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.upsertItem(
                itemCode,
                displayName,
                null,
                sortOrder,
                enabled,
                false,
                null,
                "{\"systemEnum\":true}",
                now
        ));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView disableSystemItemsExcept(UUID typeId, Set<String> retainedItemCodes) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = dictionaryType;
        for (DictionaryItemView item : dictionaryType.toView().items()) {
            if (!retainedItemCodes.contains(item.itemCode()) && item.enabled()) {
                updatedType = updatedType.disableItem(item.id(), now);
            }
        }
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public Optional<DictionaryTypeView> queryByCode(UUID tenantId, String code) {
        Objects.requireNonNull(code, "code must not be null");
        Optional<DictionaryType> tenantScoped = tenantId == null
                ? Optional.empty()
                : dictionaryTypeRepository.findByCode(tenantId, code);
        return tenantScoped.or(() -> dictionaryTypeRepository.findByCode(null, code)).map(DictionaryType::toView);
    }

    public Optional<DictionaryTypeView> queryExactByCode(UUID tenantId, String code) {
        Objects.requireNonNull(code, "code must not be null");
        return dictionaryTypeRepository.findByCode(tenantId, code).map(DictionaryType::toView);
    }

    public List<DictionaryTypeView> listTypes(UUID tenantId) {
        return listTypes(tenantId, false);
    }

    public List<DictionaryTypeView> listTypes(UUID tenantId, boolean includeDisabled) {
        return dictionaryTypeRepository.findByTenant(tenantId).stream()
                .filter(type -> includeDisabled || type.status() == DictionaryStatus.ACTIVE)
                .map(DictionaryType::toView)
                .sorted(Comparator.comparingInt(DictionaryTypeView::sortOrder)
                        .thenComparing(DictionaryTypeView::code)
                        .thenComparing(DictionaryTypeView::id))
                .toList();
    }

    private DictionaryTypeView createType(
            String code,
            String name,
            String category,
            boolean hierarchical,
            boolean cacheable,
            Integer sortOrder,
            boolean systemManaged,
            UUID tenantId
    ) {
        ensureTypeCodeUnique(tenantId, code, null);
        Instant now = now();
        DictionaryType dictionaryType = DictionaryType.create(
                code,
                name,
                category,
                hierarchical,
                cacheable,
                sortOrder == null ? 0 : sortOrder,
                systemManaged,
                tenantId,
                now
        );
        DictionaryType savedType = dictionaryTypeRepository.save(dictionaryType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    private DictionaryTypeView addItem(
            UUID typeId,
            UUID requestTenantId,
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder,
            boolean defaultItem,
            String multiLangValue,
            String extensionJson,
            boolean allowSystemManaged
    ) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        ensureTenantWriteAccess(dictionaryType, requestTenantId);
        if (!allowSystemManaged) {
            ensureMutable(dictionaryType);
        }
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.addItem(
                itemCode,
                displayName,
                parentItemId,
                sortOrder,
                defaultItem,
                multiLangValue,
                extensionJson,
                now
        ));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    private DictionaryType getRequiredType(UUID typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return dictionaryTypeRepository.findById(typeId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Dictionary type not found"
                ));
    }

    private void ensureTypeCodeUnique(UUID tenantId, String code, UUID currentTypeId) {
        dictionaryTypeRepository.findByCode(tenantId, code)
                .filter(existing -> currentTypeId == null || !existing.id().equals(currentTypeId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Dictionary code already exists");
                });
    }

    private void ensureMutable(DictionaryType dictionaryType) {
        if (dictionaryType.systemManaged()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "System dictionary is read-only"
            );
        }
    }

    private void ensureTenantWriteAccess(DictionaryType dictionaryType, UUID requestTenantId) {
        if (requestTenantId == null) {
            return;
        }
        if (!requestTenantId.equals(dictionaryType.tenantId())) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Dictionary type not found");
        }
    }

    private DictionaryType applyDomainChange(DomainChange domainChange) {
        try {
            return domainChange.apply();
        } catch (IllegalArgumentException ex) {
            throw toBizException(ex);
        }
    }

    private BizException toBizException(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Dictionary operation rejected" : ex.getMessage();
        if (message.contains("already exists")) {
            return new BizException(SharedErrorDescriptors.CONFLICT, message, ex);
        }
        if (message.contains("not found")) {
            return new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, message, ex);
        }
        return new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, message, ex);
    }

    private void publishUpdatedEvent(DictionaryType dictionaryType, Instant now) {
        domainEventPublisher.publish(DictionaryTypeUpdatedEvent.from(dictionaryType, now));
        dictionaryCacheService.invalidate(dictionaryType.tenantId(), dictionaryType.code());
    }

    private Instant now() {
        return clock.instant();
    }

    @FunctionalInterface
    private interface DomainChange {

        DictionaryType apply();
    }
}
