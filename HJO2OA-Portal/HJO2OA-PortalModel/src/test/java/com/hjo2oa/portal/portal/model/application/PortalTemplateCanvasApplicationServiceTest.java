package com.hjo2oa.portal.portal.model.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegionView;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPageView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalTemplateCanvasApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T14:00:00Z");

    @Test
    void shouldReturnHomeCanvasSkeleton() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-home",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));

        PortalTemplateCanvasView canvas = fixture.applicationService().current("template-home").orElseThrow();

        assertThat(canvas.sceneType()).isEqualTo(PortalPublicationSceneType.HOME);
        assertThat(canvas.latestVersionNo()).isEqualTo(1);
        assertThat(canvas.publishedVersionNo()).isNull();
        assertThat(canvas.pages()).hasSize(1);

        PortalPageView page = canvas.pages().get(0);
        assertThat(page.pageCode()).isEqualTo("home-main");
        assertThat(page.layoutMode()).isEqualTo(PortalTemplateLayoutMode.THREE_SECTION);
        assertThat(page.regions()).extracting(PortalLayoutRegionView::regionCode)
                .containsExactly("identity-overview", "work-focus");
        assertThat(page.regions().get(0).placements()).singleElement().satisfies(placement -> {
            assertThat(placement.widgetCode()).isEqualTo("identity-card");
            assertThat(placement.cardType()).isEqualTo(WidgetCardType.IDENTITY);
        });
        assertThat(page.regions().get(1).placements()).extracting(placement -> placement.widgetCode())
                .containsExactly("todo-card", "message-card");
    }

    @Test
    void shouldReturnOfficeCenterCanvasSkeleton() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-office",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));

        PortalTemplateCanvasView canvas = fixture.applicationService().current("template-office").orElseThrow();
        PortalPageView page = canvas.pages().get(0);

        assertThat(canvas.sceneType()).isEqualTo(PortalPublicationSceneType.OFFICE_CENTER);
        assertThat(page.pageCode()).isEqualTo("office-main");
        assertThat(page.layoutMode()).isEqualTo(PortalTemplateLayoutMode.OFFICE_SPLIT);
        assertThat(page.regions()).extracting(PortalLayoutRegionView::regionCode)
                .containsExactly("left-panel", "right-panel");
        assertThat(page.regions().get(1).placements()).extracting(placement -> placement.cardType())
                .containsExactly(WidgetCardType.TODO, WidgetCardType.MESSAGE);
    }

    @Test
    void shouldReturnMobileWorkbenchCanvasSkeleton() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-mobile",
                "mobile-default",
                "Mobile Default",
                PortalPublicationSceneType.MOBILE_WORKBENCH
        ));

        PortalTemplateCanvasView canvas = fixture.applicationService().current("template-mobile").orElseThrow();
        PortalPageView page = canvas.pages().get(0);

        assertThat(canvas.sceneType()).isEqualTo(PortalPublicationSceneType.MOBILE_WORKBENCH);
        assertThat(page.pageCode()).isEqualTo("mobile-main");
        assertThat(page.layoutMode()).isEqualTo(PortalTemplateLayoutMode.MOBILE_LIGHT);
        assertThat(page.regions()).singleElement().satisfies(region -> {
            assertThat(region.regionCode()).isEqualTo("mobile-stack");
            assertThat(region.placements()).extracting(placement -> placement.widgetCode())
                    .containsExactly("todo-card", "message-card", "identity-card");
        });
    }

    @Test
    void shouldReturnEmptyWhenTemplateDoesNotExist() {
        TestFixture fixture = fixture();

        assertThat(fixture.applicationService().current("missing-template")).isEmpty();
    }

    @Test
    void shouldSaveCanvasAndReturnUpdatedStructure() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-home",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));

        PortalTemplateCanvasView canvas = fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-home",
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                        "page-home-custom",
                        "home-custom",
                        "Home Custom",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20),
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10)
                                )
                        ))
                ))
        ));

        assertThat(canvas.pages()).singleElement().satisfies(page -> {
            assertThat(page.pageCode()).isEqualTo("home-custom");
            assertThat(page.regions()).singleElement().satisfies(region ->
                    assertThat(region.placements()).extracting(placement -> placement.widgetCode())
                            .containsExactly("message-card", "todo-card")
            );
        });
        assertThat(fixture.templateRepository().findByTemplateId("template-home"))
                .map(PortalTemplate::updatedAt)
                .hasValue(FIXED_TIME);
    }

    @Test
    void shouldKeepPublishedCanvasSnapshotStableAfterDraftChangesUntilNextPublish() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-home",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-home",
                publishedPages()
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-home", 1));
        fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-home",
                draftPages()
        ));

        PortalTemplateCanvasView publishedCanvas = fixture.applicationService()
                .currentPublished("template-home")
                .orElseThrow();
        PortalTemplateCanvasView draftCanvas = fixture.applicationService().current("template-home").orElseThrow();

        assertThat(publishedCanvas.pages()).singleElement().satisfies(page -> {
            assertThat(page.pageCode()).isEqualTo("home-published");
            assertThat(page.regions()).singleElement().satisfies(region ->
                    assertThat(region.placements()).extracting(placement -> placement.widgetCode())
                            .containsExactly("message-card", "todo-card")
            );
        });
        assertThat(draftCanvas.pages()).singleElement().satisfies(page ->
                assertThat(page.pageCode()).isEqualTo("home-draft")
        );

        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-home", 2));

        PortalTemplateCanvasView republishedCanvas = fixture.applicationService()
                .currentPublished("template-home")
                .orElseThrow();

        assertThat(republishedCanvas.pages()).singleElement().satisfies(page -> {
            assertThat(page.pageCode()).isEqualTo("home-draft");
            assertThat(page.regions()).singleElement().satisfies(region ->
                    assertThat(region.placements()).extracting(placement -> placement.widgetCode())
                            .containsExactly("identity-card")
            );
        });
        assertThat(republishedCanvas.publishedVersionNo()).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicatePageCodeWhenSavingCanvas() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-home",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-home",
                List.of(
                        new com.hjo2oa.portal.portal.model.domain.PortalPage(
                                "page-1",
                                "home-dup",
                                "Home One",
                                true,
                                PortalTemplateLayoutMode.THREE_SECTION,
                                List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                        "region-1",
                                        "identity-overview",
                                        "Identity Overview",
                                        true,
                                        List.of(placement("placement-1", "identity-card", WidgetCardType.IDENTITY, 10))
                                ))
                        ),
                        new com.hjo2oa.portal.portal.model.domain.PortalPage(
                                "page-2",
                                "home-dup",
                                "Home Two",
                                false,
                                PortalTemplateLayoutMode.THREE_SECTION,
                                List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                        "region-2",
                                        "work-focus",
                                        "Work Focus",
                                        true,
                                        List.of(placement("placement-2", "todo-card", WidgetCardType.TODO, 20))
                                ))
                        )
                )
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Duplicate page code");
    }

    @Test
    void shouldRejectMissingTemplateWhenSavingCanvas() {
        TestFixture fixture = fixture();

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "missing-template",
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                        "page-home-custom",
                        "home-custom",
                        "Home Custom",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10))
                        ))
                ))
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Portal template not found");
    }

    @Test
    void shouldRejectRepairRequiredWidgetReferenceWhenSavingCanvas() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-home",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
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

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-home",
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                        "page-home-custom",
                        "home-custom",
                        "Home Custom",
                        true,
                        PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10))
                        ))
                ))
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("repair-required widget references")
                .hasMessageContaining("widgetCode=message-card")
                .hasMessageContaining("placementCode=placement-message");
    }

    private TestFixture fixture() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        return new TestFixture(
                repository,
                templateApplicationService,
                widgetReferenceStatusApplicationService,
                new PortalTemplateCanvasApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                        widgetReferenceStatusApplicationService
                )
        );
    }

    private com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement placement(
            String placementCode,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement(
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

    private List<com.hjo2oa.portal.portal.model.domain.PortalPage> publishedPages() {
        return List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                "page-home-published",
                "home-published",
                "Home Published",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                        "region-work-focus",
                        "work-focus",
                        "Work Focus",
                        true,
                        List.of(
                                placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                        )
                ))
        ));
    }

    private List<com.hjo2oa.portal.portal.model.domain.PortalPage> draftPages() {
        return List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                "page-home-draft",
                "home-draft",
                "Home Draft",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                        "region-identity-overview",
                        "identity-overview",
                        "Identity Overview",
                        true,
                        List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 10))
                ))
        ));
    }

    private record TestFixture(
            InMemoryPortalTemplateRepository templateRepository,
            PortalTemplateApplicationService templateApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalTemplateCanvasApplicationService applicationService
    ) {
    }
}
