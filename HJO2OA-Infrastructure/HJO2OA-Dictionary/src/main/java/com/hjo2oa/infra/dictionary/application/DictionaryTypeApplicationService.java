package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.dictionary.domain.DictionaryType;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeRepository;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeUpdatedEvent;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DictionaryTypeApplicationService {

    private final DictionaryTypeRepository dictionaryTypeRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(dictionaryTypeRepository, domainEventPublisher, Clock.systemUTC());
    }
    public DictionaryTypeApplicationService(
            DictionaryTypeRepository dictionaryTypeRepository,
            DomainEventPublisher domainEventPublisher,
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
        ensureTypeCodeUnique(tenantId, code, null);
        Instant now = now();
        DictionaryType dictionaryType = DictionaryType.create(
                code,
                name,
                category,
                hierarchical,
                cacheable,
                tenantId,
                now
        );
        DictionaryType savedType = dictionaryTypeRepository.save(dictionaryType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView disableType(UUID typeId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
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
        DictionaryType dictionaryType = getRequiredType(typeId);
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
        Objects.requireNonNull(command, "command must not be null");
        return addItem(typeId, command.itemCode(), command.displayName(), command.parentItemId(), command.sortOrder());
    }

    public DictionaryTypeView addItem(
            UUID typeId,
            String itemCode,
            String displayName,
            UUID parentItemId,
            Integer sortOrder
    ) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(
                () -> dictionaryType.addItem(itemCode, displayName, parentItemId, sortOrder, now)
        );
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView updateItem(UUID typeId, UUID itemId, DictionaryTypeCommands.UpdateItemCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return updateItem(typeId, itemId, command.displayName(), command.sortOrder());
    }

    public DictionaryTypeView updateItem(UUID typeId, UUID itemId, String displayName, Integer sortOrder) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.updateItem(itemId, displayName, sortOrder, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView removeItem(UUID typeId, UUID itemId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.removeItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView enableItem(UUID typeId, UUID itemId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.enableItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public DictionaryTypeView disableItem(UUID typeId, UUID itemId) {
        DictionaryType dictionaryType = getRequiredType(typeId);
        Instant now = now();
        DictionaryType updatedType = applyDomainChange(() -> dictionaryType.disableItem(itemId, now));
        DictionaryType savedType = dictionaryTypeRepository.save(updatedType);
        publishUpdatedEvent(savedType, now);
        return savedType.toView();
    }

    public Optional<DictionaryTypeView> queryByCode(UUID tenantId, String code) {
        Objects.requireNonNull(code, "code must not be null");
        return dictionaryTypeRepository.findByCode(tenantId, code).map(DictionaryType::toView);
    }

    public List<DictionaryTypeView> listTypes(UUID tenantId) {
        return listTypes(tenantId, false);
    }

    public List<DictionaryTypeView> listTypes(UUID tenantId, boolean includeDisabled) {
        List<DictionaryType> dictionaryTypes = tenantId == null
                ? (includeDisabled ? dictionaryTypeRepository.findByTenant(null) : dictionaryTypeRepository.findAllActive())
                : dictionaryTypeRepository.findByTenant(tenantId);
        return dictionaryTypes.stream()
                .map(DictionaryType::toView)
                .toList();
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
    }

    private Instant now() {
        return clock.instant();
    }

    @FunctionalInterface
    private interface DomainChange {

        DictionaryType apply();
    }
}
