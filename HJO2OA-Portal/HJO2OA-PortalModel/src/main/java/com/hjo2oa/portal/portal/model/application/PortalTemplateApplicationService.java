package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateRepository;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateVersionStatus;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalTemplateApplicationService {

    private final PortalTemplateRepository templateRepository;
    private final PortalModelContextProvider contextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService;

    public PortalTemplateApplicationService(
            PortalTemplateRepository templateRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                templateRepository,
                contextProvider,
                domainEventPublisher,
                Clock.systemUTC(),
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    @Autowired
    public PortalTemplateApplicationService(
            PortalTemplateRepository templateRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this(
                templateRepository,
                contextProvider,
                domainEventPublisher,
                Clock.systemUTC(),
                widgetReferenceStatusApplicationService
        );
    }

    public PortalTemplateApplicationService(
            PortalTemplateRepository templateRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(
                templateRepository,
                contextProvider,
                domainEventPublisher,
                clock,
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    public PortalTemplateApplicationService(
            PortalTemplateRepository templateRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository must not be null");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.widgetReferenceStatusApplicationService = Objects.requireNonNull(
                widgetReferenceStatusApplicationService,
                "widgetReferenceStatusApplicationService must not be null"
        );
    }

    public Optional<PortalTemplateView> current(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return templateRepository.findByTemplateId(templateId).map(PortalTemplate::toView);
    }

    public List<PortalTemplateView> list(PortalPublicationSceneType sceneType) {
        PortalModelContext context = contextProvider.currentContext();
        return templateRepository.findAllByTenant(context.tenantId()).stream()
                .filter(template -> sceneType == null || template.sceneType() == sceneType)
                .map(PortalTemplate::toView)
                .sorted(Comparator.comparing(PortalTemplateView::templateCode)
                        .thenComparing(PortalTemplateView::templateId))
                .toList();
    }

    public PortalTemplateView create(CreatePortalTemplateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalModelContext context = contextProvider.currentContext();
        ensureCodeUniqueness(context.tenantId(), command.templateCode(), command.templateId());
        Instant now = now();

        PortalTemplate existing = templateRepository.findByTemplateId(command.templateId()).orElse(null);
        PortalTemplate template = existing == null
                ? PortalTemplate.create(
                        command.templateId(),
                        context.tenantId(),
                        command.templateCode(),
                        command.displayName(),
                        command.sceneType(),
                        now
                )
                : existing;
        templateRepository.save(template);
        if (existing == null) {
            domainEventPublisher.publish(PortalTemplateCreatedEvent.from(template, now));
        }
        return template.toView();
    }

    public PortalTemplateView publish(PublishPortalTemplateVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalTemplate template = templateRepository.findByTemplateId(command.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        widgetReferenceStatusApplicationService.ensureNoRepairRequiredReferences(
                template.tenantId(),
                template.pages().stream().map(com.hjo2oa.portal.portal.model.domain.PortalPage::toView).toList()
        );
        Instant now = now();
        boolean alreadyPublished = template.version(command.versionNo())
                .map(version -> version.status() == PortalTemplateVersionStatus.PUBLISHED)
                .orElse(false);

        PortalTemplate publishedTemplate;
        try {
            publishedTemplate = template.publishVersion(command.versionNo(), now);
        } catch (IllegalStateException exception) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, exception.getMessage());
        }
        templateRepository.save(publishedTemplate);
        if (!alreadyPublished) {
            domainEventPublisher.publish(PortalTemplatePublishedEvent.from(
                    publishedTemplate,
                    command.versionNo(),
                    now
            ));
        }
        return publishedTemplate.toView();
    }

    public PortalTemplateView deprecate(DeprecatePortalTemplateVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalTemplate template = templateRepository.findByTemplateId(command.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        PortalTemplateVersionStatus currentStatus = template.version(command.versionNo())
                .map(version -> version.status())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template version not found"
                ));
        Instant now = now();

        PortalTemplate deprecatedTemplate;
        try {
            deprecatedTemplate = template.deprecateVersion(command.versionNo(), now);
        } catch (IllegalArgumentException exception) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, exception.getMessage());
        }
        templateRepository.save(deprecatedTemplate);
        if (currentStatus != PortalTemplateVersionStatus.DEPRECATED) {
            domainEventPublisher.publish(PortalTemplateDeprecatedEvent.from(
                    deprecatedTemplate,
                    command.versionNo(),
                    now
            ));
        }
        return deprecatedTemplate.toView();
    }

    private Instant now() {
        return clock.instant();
    }

    private void ensureCodeUniqueness(String tenantId, String templateCode, String templateId) {
        templateRepository.findByTemplateCode(tenantId, templateCode)
                .filter(existing -> !existing.templateId().equals(templateId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Portal template code already exists");
                });
    }
}
