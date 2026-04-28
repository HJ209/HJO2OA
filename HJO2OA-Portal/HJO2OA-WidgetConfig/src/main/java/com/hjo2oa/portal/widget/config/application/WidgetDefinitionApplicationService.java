package com.hjo2oa.portal.widget.config.application;

import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinition;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionView;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class WidgetDefinitionApplicationService {

    private final WidgetDefinitionRepository widgetDefinitionRepository;
    private final WidgetConfigContextProvider contextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public WidgetDefinitionApplicationService(
            WidgetDefinitionRepository widgetDefinitionRepository,
            WidgetConfigContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(widgetDefinitionRepository, contextProvider, domainEventPublisher, Clock.systemUTC());
    }
    public WidgetDefinitionApplicationService(
            WidgetDefinitionRepository widgetDefinitionRepository,
            WidgetConfigContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.widgetDefinitionRepository = Objects.requireNonNull(
                widgetDefinitionRepository,
                "widgetDefinitionRepository must not be null"
        );
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Optional<WidgetDefinitionView> current(String widgetId) {
        Objects.requireNonNull(widgetId, "widgetId must not be null");
        return widgetDefinitionRepository.findByWidgetId(widgetId).map(WidgetDefinition::toView);
    }

    public List<WidgetDefinitionView> list(WidgetSceneType sceneType, WidgetDefinitionStatus status) {
        WidgetConfigContext context = contextProvider.currentContext();
        return widgetDefinitionRepository.findAllByTenant(context.tenantId()).stream()
                .filter(widgetDefinition -> sceneType == null || widgetDefinition.sceneType() == sceneType)
                .filter(widgetDefinition -> status == null || widgetDefinition.status() == status)
                .map(WidgetDefinition::toView)
                .sorted(Comparator.comparing(WidgetDefinitionView::widgetCode)
                        .thenComparing(WidgetDefinitionView::widgetId))
                .toList();
    }

    public WidgetDefinitionView upsert(UpsertWidgetDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WidgetConfigContext context = contextProvider.currentContext();
        Instant now = now();

        WidgetDefinition existing = widgetDefinitionRepository.findByWidgetId(command.widgetId()).orElse(null);
        ensureCodeUniqueness(context.tenantId(), command.widgetCode(), command.widgetId());

        WidgetDefinition widgetDefinition = existing == null
                ? WidgetDefinition.create(
                        command.widgetId(),
                        context.tenantId(),
                        command.widgetCode(),
                        command.displayName(),
                        command.cardType(),
                        command.sceneType(),
                        command.sourceModule(),
                        command.dataSourceType(),
                        command.allowHide(),
                        command.allowCollapse(),
                        command.maxItems(),
                        now
                )
                : existing.update(
                        command.widgetCode(),
                        command.displayName(),
                        command.cardType(),
                        command.sceneType(),
                        command.sourceModule(),
                        command.dataSourceType(),
                        command.allowHide(),
                        command.allowCollapse(),
                        command.maxItems(),
                        now
                );

        List<String> changedFields = changedFields(existing, widgetDefinition);
        widgetDefinitionRepository.save(widgetDefinition);
        if (!changedFields.isEmpty()) {
            domainEventPublisher.publish(PortalWidgetUpdatedEvent.from(existing, widgetDefinition, changedFields, now));
        }
        return widgetDefinition.toView();
    }

    public WidgetDefinitionView disable(DisableWidgetDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WidgetDefinition widgetDefinition = widgetDefinitionRepository.findByWidgetId(command.widgetId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Widget definition not found"
                ));
        if (widgetDefinition.status() == WidgetDefinitionStatus.DISABLED) {
            return widgetDefinition.toView();
        }

        WidgetDefinition disabledDefinition = widgetDefinition.disable(now());
        widgetDefinitionRepository.save(disabledDefinition);
        domainEventPublisher.publish(PortalWidgetDisabledEvent.from(disabledDefinition, now()));
        return disabledDefinition.toView();
    }

    private Instant now() {
        return clock.instant();
    }

    private void ensureCodeUniqueness(String tenantId, String widgetCode, String widgetId) {
        widgetDefinitionRepository.findByWidgetCode(tenantId, widgetCode)
                .filter(existingByCode -> !existingByCode.widgetId().equals(widgetId))
                .ifPresent(existingByCode -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Widget code already exists");
                });
    }

    private List<String> changedFields(WidgetDefinition before, WidgetDefinition after) {
        List<String> changedFields = new ArrayList<>();
        if (before == null) {
            changedFields.add("widgetCode");
            changedFields.add("displayName");
            changedFields.add("cardType");
            changedFields.add("sceneType");
            changedFields.add("sourceModule");
            changedFields.add("dataSourceType");
            changedFields.add("allowHide");
            changedFields.add("allowCollapse");
            changedFields.add("maxItems");
            changedFields.add("status");
            return List.copyOf(changedFields);
        }

        if (!before.widgetCode().equals(after.widgetCode())) {
            changedFields.add("widgetCode");
        }
        if (!before.displayName().equals(after.displayName())) {
            changedFields.add("displayName");
        }
        if (before.cardType() != after.cardType()) {
            changedFields.add("cardType");
        }
        if (!Objects.equals(before.sceneType(), after.sceneType())) {
            changedFields.add("sceneType");
        }
        if (!before.sourceModule().equals(after.sourceModule())) {
            changedFields.add("sourceModule");
        }
        if (before.dataSourceType() != after.dataSourceType()) {
            changedFields.add("dataSourceType");
        }
        if (before.allowHide() != after.allowHide()) {
            changedFields.add("allowHide");
        }
        if (before.allowCollapse() != after.allowCollapse()) {
            changedFields.add("allowCollapse");
        }
        if (before.maxItems() != after.maxItems()) {
            changedFields.add("maxItems");
        }
        if (before.status() != after.status()) {
            changedFields.add("status");
        }
        return List.copyOf(changedFields);
    }
}
