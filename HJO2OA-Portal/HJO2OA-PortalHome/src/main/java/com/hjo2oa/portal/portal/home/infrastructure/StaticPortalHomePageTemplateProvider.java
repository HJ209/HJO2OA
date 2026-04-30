package com.hjo2oa.portal.portal.home.infrastructure;

import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeBranding;
import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeFooter;
import com.hjo2oa.portal.portal.home.domain.PortalHomeLayoutType;
import com.hjo2oa.portal.portal.home.domain.PortalHomeNavigationItem;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplateProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StaticPortalHomePageTemplateProvider implements PortalHomePageTemplateProvider {

    private final Map<PortalHomeSceneType, PortalHomePageTemplate> templates = buildTemplates();

    @Override
    public PortalHomePageTemplate templateFor(PortalHomeSceneType sceneType) {
        return templates.get(sceneType);
    }

    private Map<PortalHomeSceneType, PortalHomePageTemplate> buildTemplates() {
        Map<PortalHomeSceneType, PortalHomePageTemplate> definedTemplates = new EnumMap<>(PortalHomeSceneType.class);
        definedTemplates.put(PortalHomeSceneType.HOME, homeTemplate());
        definedTemplates.put(PortalHomeSceneType.OFFICE_CENTER, officeCenterTemplate());
        definedTemplates.put(PortalHomeSceneType.MOBILE_WORKBENCH, mobileTemplate());
        return definedTemplates;
    }

    private PortalHomePageTemplate homeTemplate() {
        return new PortalHomePageTemplate(
                PortalHomeSceneType.HOME,
                PortalHomeLayoutType.THREE_SECTION,
                new PortalHomeBranding("HJO2OA Workspace", "Unified workbench for approvals and communication", "HJO2OA"),
                List.of(
                        new PortalHomeNavigationItem("home", "Home", "/portal/home"),
                        new PortalHomeNavigationItem("office-center", "Office Center", "/portal/home/page?sceneType=OFFICE_CENTER"),
                        new PortalHomeNavigationItem("mobile", "Mobile", "/portal/home/page?sceneType=MOBILE_WORKBENCH")
                ),
                List.of(
                        new PortalHomeRegionTemplate(
                                "identity-overview",
                                "Identity Overview",
                                "Current assignment and role context",
                                List.of(new PortalHomeCardTemplate(
                                        "identity-card",
                                        PortalCardType.IDENTITY,
                                        "Current Identity",
                                        "Primary identity context for the active session",
                                        "/api/v1/org/identity-context/current"
                                ))
                        ),
                        new PortalHomeRegionTemplate(
                                "work-focus",
                                "Work Focus",
                                "High-frequency work cards for the current identity",
                                List.of(
                                        new PortalHomeCardTemplate(
                                                "todo-card",
                                                PortalCardType.TODO,
                                                "Pending Tasks",
                                                "Pending items that require immediate action",
                                                "/api/v1/todo/pending"
                                        ),
                                        new PortalHomeCardTemplate(
                                                "message-card",
                                                PortalCardType.MESSAGE,
                                                "Unread Messages",
                                                "Latest reminders and message center updates",
                                                "/api/v1/msg/messages"
                                        )
                                )
                        )
                ),
                new PortalHomeFooter("Powered by HJO2OA portal-home assembly layer")
        );
    }

    private PortalHomePageTemplate officeCenterTemplate() {
        return new PortalHomePageTemplate(
                PortalHomeSceneType.OFFICE_CENTER,
                PortalHomeLayoutType.OFFICE_SPLIT,
                new PortalHomeBranding("Office Center", "Focused processing workspace", "OFFICE"),
                List.of(
                        new PortalHomeNavigationItem("pending", "Pending", "/api/v1/todo/pending"),
                        new PortalHomeNavigationItem("messages", "Messages", "/api/v1/msg/messages"),
                        new PortalHomeNavigationItem("identity", "Identity", "/api/v1/org/identity-context/current")
                ),
                List.of(
                        new PortalHomeRegionTemplate(
                                "left-panel",
                                "Work Navigation",
                                "Identity summary and quick navigation",
                                List.of(new PortalHomeCardTemplate(
                                        "office-identity",
                                        PortalCardType.IDENTITY,
                                        "Active Assignment",
                                        "Current assignment context for office work",
                                        "/api/v1/org/identity-context/current"
                                ))
                        ),
                        new PortalHomeRegionTemplate(
                                "right-panel",
                                "Processing Queue",
                                "Current tasks and reminders for the active identity",
                                List.of(
                                        new PortalHomeCardTemplate(
                                                "office-todo",
                                                PortalCardType.TODO,
                                                "My Pending Queue",
                                                "Pending tasks ordered for quick processing",
                                                "/api/v1/todo/pending"
                                        ),
                                        new PortalHomeCardTemplate(
                                                "office-message",
                                                PortalCardType.MESSAGE,
                                                "Reminder Stream",
                                                "Latest reminder stream from message center",
                                                "/api/v1/msg/messages"
                                        )
                                )
                        )
                ),
                new PortalHomeFooter("Office center page assembled from static template and aggregation snapshots")
        );
    }

    private PortalHomePageTemplate mobileTemplate() {
        return new PortalHomePageTemplate(
                PortalHomeSceneType.MOBILE_WORKBENCH,
                PortalHomeLayoutType.MOBILE_LIGHT,
                new PortalHomeBranding("Mobile Workbench", "Compact workbench for high-frequency actions", "MOBILE"),
                List.of(
                        new PortalHomeNavigationItem("tasks", "Tasks", "/api/v1/todo/pending"),
                        new PortalHomeNavigationItem("messages", "Messages", "/api/v1/msg/messages")
                ),
                List.of(
                        new PortalHomeRegionTemplate(
                                "mobile-stack",
                                "Mobile Cards",
                                "Compact stack for current work context",
                                List.of(
                                        new PortalHomeCardTemplate(
                                                "mobile-todo",
                                                PortalCardType.TODO,
                                                "Tasks",
                                                "Mobile-first pending task card",
                                                "/api/v1/todo/pending"
                                        ),
                                        new PortalHomeCardTemplate(
                                                "mobile-message",
                                                PortalCardType.MESSAGE,
                                                "Messages",
                                                "Mobile-first reminder card",
                                                "/api/v1/msg/messages"
                                        ),
                                        new PortalHomeCardTemplate(
                                                "mobile-identity",
                                                PortalCardType.IDENTITY,
                                                "Identity",
                                                "Current assignment context",
                                                "/api/v1/org/identity-context/current"
                                        )
                                )
                        )
                ),
                new PortalHomeFooter("Mobile workbench is optimized for high-frequency page assembly")
        );
    }
}
