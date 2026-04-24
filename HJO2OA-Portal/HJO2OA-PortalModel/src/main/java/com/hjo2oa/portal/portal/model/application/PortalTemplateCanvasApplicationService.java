package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalTemplateCanvasApplicationService {

    private final PortalTemplateRepository templateRepository;
    private final Clock clock;
    private final PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService;

    public PortalTemplateCanvasApplicationService(PortalTemplateRepository templateRepository) {
        this(templateRepository, Clock.systemUTC(), PortalWidgetReferenceStatusApplicationService.noop());
    }

    @Autowired
    public PortalTemplateCanvasApplicationService(
            PortalTemplateRepository templateRepository,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this(templateRepository, Clock.systemUTC(), widgetReferenceStatusApplicationService);
    }

    public PortalTemplateCanvasApplicationService(
            PortalTemplateRepository templateRepository,
            Clock clock
    ) {
        this(templateRepository, clock, PortalWidgetReferenceStatusApplicationService.noop());
    }

    public PortalTemplateCanvasApplicationService(
            PortalTemplateRepository templateRepository,
            Clock clock,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.widgetReferenceStatusApplicationService = Objects.requireNonNull(
                widgetReferenceStatusApplicationService,
                "widgetReferenceStatusApplicationService must not be null"
        );
    }

    public Optional<PortalTemplateCanvasView> current(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return templateRepository.findByTemplateId(templateId).map(PortalTemplate::toCanvasView);
    }

    public Optional<PortalTemplateCanvasView> currentPublished(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return templateRepository.findByTemplateId(templateId)
                .flatMap(PortalTemplate::toPublishedCanvasView);
    }

    public PortalTemplateCanvasView save(SavePortalTemplateCanvasCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalTemplate template = templateRepository.findByTemplateId(command.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal template not found"
                ));
        widgetReferenceStatusApplicationService.ensureNoRepairRequiredReferences(
                template.tenantId(),
                command.pages().stream().map(PortalPage::toView).toList()
        );
        PortalTemplate updatedTemplate;
        try {
            updatedTemplate = template.replaceCanvas(command.pages(), now());
        } catch (IllegalArgumentException exception) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, exception.getMessage());
        }
        templateRepository.save(updatedTemplate);
        return updatedTemplate.toCanvasView();
    }

    private Instant now() {
        return clock.instant();
    }
}
