package com.hjo2oa.portal.widget.config.interfaces;

import com.hjo2oa.portal.widget.config.application.DisableWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/portal/widget-config/widgets")
public class WidgetConfigController {

    private final WidgetDefinitionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public WidgetConfigController(
            WidgetDefinitionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/{widgetId}")
    public ApiResponse<WidgetDefinitionView> current(
            @PathVariable String widgetId,
            HttpServletRequest request
    ) {
        WidgetDefinitionView widgetDefinition = applicationService.current(widgetId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Widget definition not found"));
        return ApiResponse.success(widgetDefinition, responseMetaFactory.create(request));
    }

    @GetMapping
    public ApiResponse<List<WidgetDefinitionView>> list(
            @RequestParam(required = false) com.hjo2oa.portal.widget.config.domain.WidgetSceneType sceneType,
            @RequestParam(required = false) com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.list(sceneType, status),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{widgetId}")
    public ApiResponse<WidgetDefinitionView> upsert(
            @PathVariable String widgetId,
            @Valid @RequestBody UpsertWidgetDefinitionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.upsert(body.toCommand(widgetId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{widgetId}/disable")
    public ApiResponse<WidgetDefinitionView> disable(
            @PathVariable String widgetId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.disable(new DisableWidgetDefinitionCommand(widgetId)),
                responseMetaFactory.create(request)
        );
    }
}
