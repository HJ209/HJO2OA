package com.hjo2oa.portal.aggregation.api.interfaces;

import com.hjo2oa.portal.aggregation.api.application.PortalDashboardAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.application.PortalMessageListAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.application.PortalOfficeCenterAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageListView;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/aggregation")
public class PortalAggregationController {

    private final PortalDashboardAggregationApplicationService aggregationApplicationService;
    private final PortalOfficeCenterAggregationApplicationService officeCenterAggregationApplicationService;
    private final PortalMessageListAggregationApplicationService messageListAggregationApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalAggregationController(
            PortalDashboardAggregationApplicationService aggregationApplicationService,
            PortalOfficeCenterAggregationApplicationService officeCenterAggregationApplicationService,
            PortalMessageListAggregationApplicationService messageListAggregationApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.aggregationApplicationService = aggregationApplicationService;
        this.officeCenterAggregationApplicationService = officeCenterAggregationApplicationService;
        this.messageListAggregationApplicationService = messageListAggregationApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/dashboard")
    public ApiResponse<PortalDashboardView> dashboard(
            @RequestParam(name = "sceneType", defaultValue = "HOME") PortalSceneType sceneType,
            @RequestParam(name = "cards", required = false) List<PortalCardType> cards,
            HttpServletRequest request
    ) {
        PortalDashboardView dashboard = aggregationApplicationService.dashboard(sceneType, toRequestedCards(cards));
        return ApiResponse.success(dashboard, responseMetaFactory.create(request));
    }

    @GetMapping("/office-center")
    public ApiResponse<PortalOfficeCenterView> officeCenter(HttpServletRequest request) {
        PortalOfficeCenterView officeCenter = officeCenterAggregationApplicationService.officeCenter();
        return ApiResponse.success(officeCenter, responseMetaFactory.create(request));
    }

    @GetMapping("/office-center/messages")
    public ApiResponse<PortalMessageListView> officeCenterMessages(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "messageCategory", required = false) NotificationCategory messageCategory,
            @RequestParam(name = "readStatus", required = false) NotificationInboxStatus readStatus,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest request
    ) {
        PortalMessageListView messageList = messageListAggregationApplicationService.officeCenterMessages(
                page,
                size,
                messageCategory,
                readStatus,
                keyword
        );
        return ApiResponse.success(messageList, responseMetaFactory.create(request));
    }

    @GetMapping("/card/{cardType}")
    public ApiResponse<PortalCardSnapshot<?>> card(
            @PathVariable PortalCardType cardType,
            @RequestParam(name = "sceneType", defaultValue = "HOME") PortalSceneType sceneType,
            HttpServletRequest request
    ) {
        PortalCardSnapshot<?> snapshot = aggregationApplicationService.refreshCard(sceneType, cardType);
        return ApiResponse.success(snapshot, responseMetaFactory.create(request));
    }

    private Set<PortalCardType> toRequestedCards(List<PortalCardType> cards) {
        if (cards == null || cards.isEmpty()) {
            return EnumSet.noneOf(PortalCardType.class);
        }
        return EnumSet.copyOf(cards);
    }
}
