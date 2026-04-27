package com.hjo2oa.msg.event.subscription.interfaces;

import com.hjo2oa.msg.event.subscription.application.EventSubscriptionApplicationService;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/msg/event-subscription")
public class EventSubscriptionController {

    private final EventSubscriptionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public EventSubscriptionController(
            EventSubscriptionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/admin/rules")
    public ApiResponse<EventSubscriptionDtos.RuleResponse> createRule(
            @Valid @RequestBody EventSubscriptionDtos.SaveRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                EventSubscriptionDtos.RuleResponse.from(applicationService.createRule(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/admin/rules/{ruleId}")
    public ApiResponse<EventSubscriptionDtos.RuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody EventSubscriptionDtos.UpdateRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                EventSubscriptionDtos.RuleResponse.from(applicationService.updateRule(ruleId, body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/admin/rules/{ruleId}/toggle")
    public ApiResponse<EventSubscriptionDtos.RuleResponse> toggleRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody EventSubscriptionDtos.ToggleRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                EventSubscriptionDtos.RuleResponse.from(applicationService.toggleRule(ruleId, body.enabled())),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/admin/rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@PathVariable UUID ruleId, HttpServletRequest request) {
        applicationService.deleteRule(ruleId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/admin/rules/{ruleId}")
    public ApiResponse<EventSubscriptionDtos.RuleResponse> getRule(
            @PathVariable UUID ruleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                EventSubscriptionDtos.RuleResponse.from(applicationService.getRule(ruleId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/admin/rules")
    public ApiResponse<List<EventSubscriptionDtos.RuleResponse>> listRules(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listRules(tenantId).stream()
                        .map(EventSubscriptionDtos.RuleResponse::from)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/preferences")
    public ApiResponse<List<EventSubscriptionDtos.PreferenceResponse>> listPreferences(
            @RequestParam UUID personId,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listPreferences(personId, tenantId).stream()
                        .map(EventSubscriptionDtos.PreferenceResponse::from)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/preferences/{category}")
    public ApiResponse<EventSubscriptionDtos.PreferenceResponse> savePreference(
            @PathVariable NotificationCategory category,
            @Valid @RequestBody EventSubscriptionDtos.SavePreferenceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                EventSubscriptionDtos.PreferenceResponse.from(applicationService.savePreference(body.toCommand(category))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/admin/events/match")
    public ApiResponse<List<EventSubscriptionDtos.EventMatchResponse>> matchEvent(
            @Valid @RequestBody EventSubscriptionDtos.MatchEventRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.matchEvent(body.toQuery()).stream()
                        .map(EventSubscriptionDtos.EventMatchResponse::from)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
