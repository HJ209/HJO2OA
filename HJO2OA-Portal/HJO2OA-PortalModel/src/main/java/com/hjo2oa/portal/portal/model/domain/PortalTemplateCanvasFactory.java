package com.hjo2oa.portal.portal.model.domain;

import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.util.List;
import java.util.Map;

final class PortalTemplateCanvasFactory {

    private PortalTemplateCanvasFactory() {
    }

    static List<PortalPage> defaultPages(PortalPublicationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> List.of(new PortalPage(
                    "page-home-main",
                    "home-main",
                    "Home Main",
                    true,
                    PortalTemplateLayoutMode.THREE_SECTION,
                    List.of(
                            new PortalLayoutRegion(
                                    "region-home-identity",
                                    "identity-overview",
                                    "Identity Overview",
                                    true,
                                    List.of(placement(
                                            "placement-home-identity",
                                            "identity-card",
                                            WidgetCardType.IDENTITY,
                                            10
                                    ))
                            ),
                            new PortalLayoutRegion(
                                    "region-home-work",
                                    "work-focus",
                                    "Work Focus",
                                    true,
                                    List.of(
                                            placement("placement-home-todo", "todo-card", WidgetCardType.TODO, 20),
                                            placement("placement-home-message", "message-card", WidgetCardType.MESSAGE, 30)
                                    )
                            )
                    )
            ));
            case OFFICE_CENTER -> List.of(new PortalPage(
                    "page-office-main",
                    "office-main",
                    "Office Center Main",
                    true,
                    PortalTemplateLayoutMode.OFFICE_SPLIT,
                    List.of(
                            new PortalLayoutRegion(
                                    "region-office-left",
                                    "left-panel",
                                    "Work Navigation",
                                    true,
                                    List.of(placement(
                                            "placement-office-identity",
                                            "identity-card",
                                            WidgetCardType.IDENTITY,
                                            10
                                    ))
                            ),
                            new PortalLayoutRegion(
                                    "region-office-right",
                                    "right-panel",
                                    "Processing Queue",
                                    true,
                                    List.of(
                                            placement("placement-office-todo", "todo-card", WidgetCardType.TODO, 20),
                                            placement("placement-office-message", "message-card", WidgetCardType.MESSAGE, 30)
                                    )
                            )
                    )
            ));
            case MOBILE_WORKBENCH -> List.of(new PortalPage(
                    "page-mobile-main",
                    "mobile-main",
                    "Mobile Workbench Main",
                    true,
                    PortalTemplateLayoutMode.MOBILE_LIGHT,
                    List.of(new PortalLayoutRegion(
                            "region-mobile-stack",
                            "mobile-stack",
                            "Mobile Cards",
                            true,
                            List.of(
                                    placement("placement-mobile-todo", "todo-card", WidgetCardType.TODO, 10),
                                    placement("placement-mobile-message", "message-card", WidgetCardType.MESSAGE, 20),
                                    placement("placement-mobile-identity", "identity-card", WidgetCardType.IDENTITY, 30)
                            )
                    ))
            ));
        };
    }

    private static PortalWidgetPlacement placement(
            String placementId,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new PortalWidgetPlacement(
                placementId,
                placementId,
                widgetCode,
                cardType,
                orderNo,
                false,
                false,
                Map.of()
        );
    }
}
