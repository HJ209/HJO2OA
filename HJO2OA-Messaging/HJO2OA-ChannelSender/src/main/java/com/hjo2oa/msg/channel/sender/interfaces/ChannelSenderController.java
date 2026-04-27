package com.hjo2oa.msg.channel.sender.interfaces;

import com.hjo2oa.msg.channel.sender.application.ChannelSenderApplicationService;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/msg/channel-sender")
public class ChannelSenderController {

    private final ChannelSenderApplicationService applicationService;
    private final ChannelSenderDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ChannelSenderController(
            ChannelSenderApplicationService applicationService,
            ChannelSenderDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/templates")
    public ApiResponse<ChannelSenderDtos.TemplateResponse> createTemplate(
            @Valid @RequestBody ChannelSenderDtos.CreateTemplateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTemplateResponse(applicationService.createTemplate(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/templates/{templateId}/publish")
    public ApiResponse<ChannelSenderDtos.TemplateResponse> publishTemplate(
            @PathVariable UUID templateId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTemplateResponse(applicationService.publishTemplate(templateId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/templates/{templateId}/disable")
    public ApiResponse<ChannelSenderDtos.TemplateResponse> disableTemplate(
            @PathVariable UUID templateId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTemplateResponse(applicationService.disableTemplate(templateId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/templates")
    public ApiResponse<List<ChannelSenderDtos.TemplateResponse>> templates(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String category,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listTemplates(tenantId, category).stream()
                        .map(dtoMapper::toTemplateResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/endpoints")
    public ApiResponse<ChannelSenderDtos.EndpointResponse> createEndpoint(
            @Valid @RequestBody ChannelSenderDtos.CreateEndpointRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEndpointResponse(applicationService.createEndpoint(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/endpoints/{endpointId}/status")
    public ApiResponse<ChannelSenderDtos.EndpointResponse> changeEndpointStatus(
            @PathVariable UUID endpointId,
            @Valid @RequestBody ChannelSenderDtos.ChangeEndpointStatusRequest body,
            HttpServletRequest request
    ) {
        ChannelSenderCommands.ChangeEndpointStatusCommand command =
                new ChannelSenderCommands.ChangeEndpointStatusCommand(endpointId, body.status());
        return ApiResponse.success(
                dtoMapper.toEndpointResponse(applicationService.changeEndpointStatus(command)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/endpoints")
    public ApiResponse<List<ChannelSenderDtos.EndpointResponse>> endpoints(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) ChannelType channelType,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listEndpoints(tenantId, channelType).stream()
                        .map(dtoMapper::toEndpointResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/routing-policies")
    public ApiResponse<ChannelSenderDtos.RoutingPolicyResponse> createRoutingPolicy(
            @Valid @RequestBody ChannelSenderDtos.CreateRoutingPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoutingPolicyResponse(applicationService.createRoutingPolicy(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/routing-policies/{policyId}/enable")
    public ApiResponse<ChannelSenderDtos.RoutingPolicyResponse> enableRoutingPolicy(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoutingPolicyResponse(applicationService.enableRoutingPolicy(policyId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/routing-policies/{policyId}/disable")
    public ApiResponse<ChannelSenderDtos.RoutingPolicyResponse> disableRoutingPolicy(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoutingPolicyResponse(applicationService.disableRoutingPolicy(policyId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/routing-policies")
    public ApiResponse<List<ChannelSenderDtos.RoutingPolicyResponse>> routingPolicies(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) MessageCategory category,
            @RequestParam(required = false) MessagePriority priority,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listRoutingPolicies(tenantId, category, priority).stream()
                        .map(dtoMapper::toRoutingPolicyResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/delivery-tasks")
    public ApiResponse<ChannelSenderDtos.DeliveryTaskResponse> createDeliveryTask(
            @Valid @RequestBody ChannelSenderDtos.CreateDeliveryTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDeliveryTaskResponse(applicationService.createDeliveryTask(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/delivery-tasks/route")
    public ApiResponse<List<ChannelSenderDtos.DeliveryTaskResponse>> routeDeliveryTasks(
            @Valid @RequestBody ChannelSenderDtos.RouteDeliveryTasksRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.routeDeliveryTasks(body.toCommand()).stream()
                        .map(dtoMapper::toDeliveryTaskResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/delivery-tasks/{deliveryTaskId}/attempts")
    public ApiResponse<ChannelSenderDtos.DeliveryTaskResponse> recordDeliveryResult(
            @PathVariable UUID deliveryTaskId,
            @Valid @RequestBody ChannelSenderDtos.RecordDeliveryResultRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDeliveryTaskResponse(applicationService.recordDeliveryResult(body.toCommand(deliveryTaskId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/delivery-tasks/retryable")
    public ApiResponse<List<ChannelSenderDtos.DeliveryTaskResponse>> retryableTasks(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.retryableTasks(tenantId).stream()
                        .map(dtoMapper::toDeliveryTaskResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
