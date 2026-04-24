package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerPreviewIdentityContext;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePreviewView;
import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.infrastructure.InMemoryPersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplatePreviewApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T21:00:00Z");

    @Test
    void shouldAssemblePreviewFromSavedDraftCanvas() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                                )
                        ))
                ))
        ));

        PortalDesignerTemplatePreviewView preview = fixture.applicationService().preview(
                "template-1",
                PortalPublicationClientType.PC,
                null
        );

        assertThat(preview.templateId()).isEqualTo("template-1");
        assertThat(preview.clientType()).isEqualTo(PortalPublicationClientType.PC);
        assertThat(preview.previewIdentity().assignmentId()).isEqualTo("preview-assignment-pc");
        assertThat(preview.page().sceneType().name()).isEqualTo("HOME");
        assertThat(preview.page().sourceTemplateMetadata()).isNull();
        assertThat(preview.overlay().status()).isEqualTo("bypassed");
        assertThat(preview.overlay().reason()).isEqualTo("live-publication-unresolved");
        assertThat(preview.page().regions()).singleElement().satisfies(region -> {
            assertThat(region.regionCode()).isEqualTo("work-focus");
            assertThat(region.cards()).extracting(card -> card.cardType().name())
                    .containsExactly("MESSAGE", "TODO");
            assertThat(region.cards()).allSatisfy(card -> assertThat(card.state().name()).isEqualTo("READY"));
        });
    }

    @Test
    void shouldUseSceneDefaultClientTypeWhenPreviewClientTypeIsOmitted() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-mobile",
                "mobile-default",
                "Mobile Default",
                PortalPublicationSceneType.MOBILE_WORKBENCH
        ));

        PortalDesignerTemplatePreviewView preview = fixture.applicationService().preview("template-mobile", null, null);

        assertThat(preview.clientType()).isEqualTo(PortalPublicationClientType.MOBILE);
        assertThat(preview.previewIdentity().assignmentId()).isEqualTo("preview-assignment-mobile");
        assertThat(preview.page().sceneType().name()).isEqualTo("MOBILE_WORKBENCH");
    }

    @Test
    void shouldUseExplicitPreviewIdentityContextWhenProvided() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-identity",
                "identity-default",
                "Identity Default",
                PortalPublicationSceneType.HOME
        ));

        PortalDesignerTemplatePreviewView preview = fixture.applicationService().preview(
                "template-identity",
                PortalPublicationClientType.PC,
                PortalDesignerPreviewIdentityContext.of(
                        "tenant-88",
                        "person-88",
                        "account-88",
                        "assignment-88",
                        "position-88"
                )
        );

        assertThat(preview.previewIdentity().tenantId()).isEqualTo("tenant-88");
        assertThat(preview.previewIdentity().personId()).isEqualTo("person-88");
        assertThat(preview.previewIdentity().accountId()).isEqualTo("account-88");
        assertThat(preview.previewIdentity().assignmentId()).isEqualTo("assignment-88");
        assertThat(preview.previewIdentity().positionId()).isEqualTo("position-88");
    }

    @Test
    void shouldFailWhenTemplateIsMissing() {
        TestFixture fixture = fixture();

        assertThatThrownBy(() -> fixture.applicationService().preview(
                "missing-template",
                PortalPublicationClientType.PC,
                null
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Portal template not found");
    }

    @Test
    void shouldRejectPreviewWhenDraftCanvasContainsRepairRequiredWidgetReference() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10))
                        ))
                ))
        ));
        fixture.widgetReferenceStatusApplicationService().markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-message",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));

        assertThatThrownBy(() -> fixture.applicationService().preview(
                "template-1",
                PortalPublicationClientType.PC,
                null
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("repair-required widget references")
                .hasMessageContaining("widgetCode=message-card")
                .hasMessageContaining("placementCode=placement-message");
    }

    @Test
    void shouldApplyOverlayUsingExplicitPreviewIdentityWhenBaselineMatchesResolvedLivePublication() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20),
                                        placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30)
                                )
                        ))
                ))
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-live",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-88")
        ));
        fixture.personalizationRepository().save(
                PersonalizationProfile.create(
                                "profile-1",
                                "tenant-88",
                                "person-88",
                                "assignment-88",
                                PersonalizationSceneType.HOME,
                                "publication-live",
                                FIXED_TIME
                        )
                        .saveOverrides(
                                null,
                                List.of("identity-card", "todo-card"),
                                List.of("placement-message"),
                                List.of(),
                                FIXED_TIME.plusSeconds(60)
                        )
        );

        PortalDesignerTemplatePreviewView preview = fixture.applicationService().preview(
                "template-1",
                PortalPublicationClientType.PC,
                PortalDesignerPreviewIdentityContext.of(
                        "tenant-88",
                        "person-88",
                        "account-88",
                        "assignment-88",
                        "position-88"
                )
        );

        assertThat(preview.overlay().status()).isEqualTo("applied");
        assertThat(preview.overlay().baselinePublicationId()).isEqualTo("publication-live");
        assertThat(preview.overlay().resolvedLivePublicationId()).isEqualTo("publication-live");
        assertThat(preview.overlay().reason()).isEqualTo("publication-matched");
        assertThat(preview.page().regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("IDENTITY", "TODO"));
    }

    @Test
    void shouldBypassOverlayWhenExplicitPreviewIdentityResolvesDifferentLivePublication() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.canvasApplicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new PortalPage(
                        "page-home-preview",
                        "home-preview",
                        "Home Preview",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20),
                                        placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30)
                                )
                        ))
                ))
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-live",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-88")
        ));
        fixture.personalizationRepository().save(
                PersonalizationProfile.create(
                                "profile-1",
                                "tenant-88",
                                "person-88",
                                "assignment-88",
                                PersonalizationSceneType.HOME,
                                "publication-outdated",
                                FIXED_TIME
                        )
                        .saveOverrides(
                                null,
                                List.of("identity-card"),
                                List.of("placement-message"),
                                List.of(),
                                FIXED_TIME.plusSeconds(60)
                        )
        );

        PortalDesignerTemplatePreviewView preview = fixture.applicationService().preview(
                "template-1",
                PortalPublicationClientType.PC,
                PortalDesignerPreviewIdentityContext.of(
                        "tenant-88",
                        "person-88",
                        "account-88",
                        "assignment-88",
                        "position-88"
                )
        );

        assertThat(preview.overlay().status()).isEqualTo("bypassed");
        assertThat(preview.overlay().baselinePublicationId()).isEqualTo("publication-outdated");
        assertThat(preview.overlay().resolvedLivePublicationId()).isEqualTo("publication-live");
        assertThat(preview.overlay().reason()).isEqualTo("baseline-publication-mismatch");
        assertThat(preview.page().regions()).singleElement().satisfies(region ->
                assertThat(region.cards()).extracting(card -> card.cardType().name())
                        .containsExactly("MESSAGE", "TODO", "IDENTITY"));
    }

    private TestFixture fixture() {
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        InMemoryPersonalizationProfileRepository personalizationRepository = new InMemoryPersonalizationProfileRepository();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        PortalTemplateCanvasApplicationService canvasApplicationService = new PortalTemplateCanvasApplicationService(
                templateRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                publicationRepository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalActiveTemplateResolutionApplicationService activeTemplateResolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        MutablePersonalizationBasePublicationResolver basePublicationResolver =
                new MutablePersonalizationBasePublicationResolver(sceneType -> Optional.empty());
        PersonalizationProfileApplicationService personalizationProfileApplicationService =
                new PersonalizationProfileApplicationService(
                        personalizationRepository,
                        () -> new PersonalizationIdentityContext(
                                "tenant-1",
                                "person-1",
                                "assignment-1",
                                "position-1"
                        ),
                        basePublicationResolver,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        return new TestFixture(
                templateApplicationService,
                canvasApplicationService,
                widgetReferenceStatusApplicationService,
                publicationApplicationService,
                personalizationRepository,
                new PortalDesignerTemplatePreviewApplicationService(
                        templateApplicationService,
                        canvasApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                        widgetReferenceStatusApplicationService,
                        activeTemplateResolutionApplicationService,
                        personalizationProfileApplicationService
                )
        );
    }

    private PortalWidgetPlacement placement(
            String placementCode,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new PortalWidgetPlacement(
                placementCode,
                placementCode,
                widgetCode,
                cardType,
                orderNo,
                false,
                false,
                Map.of()
        );
    }

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService canvasApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            InMemoryPersonalizationProfileRepository personalizationRepository,
            PortalDesignerTemplatePreviewApplicationService applicationService
    ) {
    }
}
