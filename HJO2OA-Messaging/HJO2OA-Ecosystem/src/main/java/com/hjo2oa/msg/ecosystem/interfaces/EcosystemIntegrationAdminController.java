package com.hjo2oa.msg.ecosystem.interfaces;

import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationApplicationService;
import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationCommands;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
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
@RequestMapping("/api/v1/msg/ecosystem/admin")
public class EcosystemIntegrationAdminController {

    private final EcosystemIntegrationApplicationService applicationService;
    private final EcosystemIntegrationDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public EcosystemIntegrationAdminController(
            EcosystemIntegrationApplicationService applicationService,
            EcosystemIntegrationDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/integrations")
    public ApiResponse<EcosystemIntegrationDtos.IntegrationResponse> createIntegration(
            @Valid @RequestBody EcosystemIntegrationDtos.CreateIntegrationRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toIntegrationResponse(applicationService.createIntegration(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/integrations/{integrationId}")
    public ApiResponse<EcosystemIntegrationDtos.IntegrationResponse> updateIntegration(
            @PathVariable UUID integrationId,
            @Valid @RequestBody EcosystemIntegrationDtos.UpdateIntegrationRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toIntegrationResponse(applicationService.updateIntegration(body.toCommand(integrationId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/integrations/{integrationId}/status")
    public ApiResponse<EcosystemIntegrationDtos.IntegrationResponse> changeStatus(
            @PathVariable UUID integrationId,
            @Valid @RequestBody EcosystemIntegrationDtos.ChangeStatusRequest body,
            HttpServletRequest request
    ) {
        EcosystemIntegrationCommands.ChangeIntegrationStatusCommand command =
                new EcosystemIntegrationCommands.ChangeIntegrationStatusCommand(integrationId, body.status());
        return ApiResponse.success(
                dtoMapper.toIntegrationResponse(applicationService.changeStatus(command)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/integrations/{integrationId}/health")
    public ApiResponse<EcosystemIntegrationDtos.IntegrationResponse> updateHealth(
            @PathVariable UUID integrationId,
            @Valid @RequestBody EcosystemIntegrationDtos.UpdateHealthRequest body,
            HttpServletRequest request
    ) {
        EcosystemIntegrationCommands.UpdateHealthCommand command =
                new EcosystemIntegrationCommands.UpdateHealthCommand(
                        integrationId,
                        body.healthStatus(),
                        body.errorSummary()
                );
        return ApiResponse.success(
                dtoMapper.toIntegrationResponse(applicationService.updateHealth(command)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/integrations/{integrationId}/test-connection")
    public ApiResponse<EcosystemIntegrationDtos.IntegrationResponse> testConnection(
            @PathVariable UUID integrationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toIntegrationResponse(applicationService.testConnection(integrationId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/integrations")
    public ApiResponse<List<EcosystemIntegrationDtos.IntegrationResponse>> integrations(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) IntegrationType integrationType,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listIntegrations(tenantId, integrationType).stream()
                        .map(dtoMapper::toIntegrationResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/integrations/{integrationId}/availability")
    public ApiResponse<EcosystemIntegrationDtos.AvailabilityResponse> availability(
            @PathVariable UUID integrationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAvailabilityResponse(applicationService.availability(integrationId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/integrations/{integrationId}/callback-audits")
    public ApiResponse<List<EcosystemIntegrationDtos.CallbackAuditResponse>> callbackAudits(
            @PathVariable UUID integrationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.callbackAudits(integrationId).stream()
                        .map(dtoMapper::toCallbackAuditResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
