package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.interfaces.PortalDesignerTemplateEventListener;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplatePublicationOfflineApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T20:00:00Z");

    @Test
    void shouldOfflinePublicationAndRefreshDesignerStatus() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        var status = fixture.applicationService().offline("template-1", "publication-1");

        assertThat(status.templateId()).isEqualTo("template-1");
        assertThat(status.hasActivePublication()).isFalse();
        assertThat(status.activePublicationIds()).isEmpty();
    }

    @Test
    void shouldRejectOfflineWhenPublicationDoesNotBelongToTemplate() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-2",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        assertThatThrownBy(() -> fixture.applicationService().offline("template-2", "publication-1"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Portal publication not found for template");
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        PortalDesignerTemplateEventListener eventListener =
                new PortalDesignerTemplateEventListener(projectionRepository);

        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                    if (event instanceof PortalTemplateCreatedEvent createdEvent) {
                        eventListener.onTemplateCreated(createdEvent);
                    } else if (event instanceof PortalTemplatePublishedEvent publishedEvent) {
                        eventListener.onTemplatePublished(publishedEvent);
                    } else if (event instanceof PortalTemplateDeprecatedEvent deprecatedEvent) {
                        eventListener.onTemplateDeprecated(deprecatedEvent);
                    }
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                    if (event instanceof PortalPublicationActivatedEvent activatedEvent) {
                        eventListener.onPublicationActivated(activatedEvent);
                    } else if (event instanceof PortalPublicationOfflinedEvent offlinedEvent) {
                        eventListener.onPublicationOfflined(offlinedEvent);
                    }
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        return new TestFixture(
                templateApplicationService,
                publicationApplicationService,
                new PortalDesignerTemplatePublicationOfflineApplicationService(
                        publicationApplicationService,
                        new PortalDesignerTemplateStatusApplicationService(projectionRepository)
                )
        );
    }

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            PortalDesignerTemplatePublicationOfflineApplicationService applicationService
    ) {
    }
}
