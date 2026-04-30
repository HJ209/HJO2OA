package com.hjo2oa.infra.timezone.interfaces;

import com.hjo2oa.infra.timezone.application.TimezoneSettingApplicationService;
import com.hjo2oa.infra.timezone.application.TimezoneSettingCommands;
import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
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
@RequestMapping("/api/v1/infra/timezone/settings")
public class TimezoneSettingController {

    private final TimezoneSettingApplicationService applicationService;
    private final TimezoneSettingDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public TimezoneSettingController(
            TimezoneSettingApplicationService applicationService,
            TimezoneSettingDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PutMapping("/system")
    public ApiResponse<TimezoneSettingDtos.TimezoneSettingResponse> setSystemDefault(
            @Valid @RequestBody TimezoneSettingDtos.SetTimezoneRequest body,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.SetSystemDefaultCommand command = body.toSystemCommand();
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.setSystemDefault(command.timezoneId())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<TimezoneSettingDtos.TimezoneSettingResponse>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) TimezoneScopeType scopeType,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listSettings(tenantId, scopeType).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/tenant/{tenantId}")
    public ApiResponse<TimezoneSettingDtos.TimezoneSettingResponse> setTenantTimezone(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TimezoneSettingDtos.SetTimezoneRequest body,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.SetTenantTimezoneCommand command = body.toTenantCommand(tenantId);
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.setTenantTimezone(command.tenantId(), command.timezoneId())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/person/{personId}")
    public ApiResponse<TimezoneSettingDtos.TimezoneSettingResponse> setPersonTimezone(
            @PathVariable UUID personId,
            @Valid @RequestBody TimezoneSettingDtos.SetTimezoneRequest body,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.SetPersonTimezoneCommand command = body.toPersonCommand(personId);
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.setPersonTimezone(command.personId(), command.timezoneId())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/resolve")
    public ApiResponse<TimezoneSettingDtos.ResolvedTimezoneResponse> resolve(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID personId,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.ResolveEffectiveTimezoneQuery query =
                new TimezoneSettingCommands.ResolveEffectiveTimezoneQuery(tenantId, personId);
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.resolveEffectiveTimezone(query.tenantId(), query.personId())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/convert/to-utc")
    public ApiResponse<TimezoneSettingDtos.ConvertToUtcResponse> convertToUtc(
            @Valid @RequestBody TimezoneSettingDtos.ConvertToUtcRequest body,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.ConvertToUtcCommand command = body.toCommand();
        return ApiResponse.success(
                dtoMapper.toConvertToUtcResponse(
                        applicationService.convertToUtc(command.localDateTime(), command.timezoneId()),
                        command.timezoneId()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/convert/from-utc")
    public ApiResponse<TimezoneSettingDtos.ConvertFromUtcResponse> convertFromUtc(
            @Valid @RequestBody TimezoneSettingDtos.ConvertFromUtcRequest body,
            HttpServletRequest request
    ) {
        TimezoneSettingCommands.ConvertFromUtcCommand command = body.toCommand();
        return ApiResponse.success(
                dtoMapper.toConvertFromUtcResponse(
                        applicationService.convertFromUtc(command.utcInstant(), command.timezoneId()),
                        command.timezoneId()
                ),
                responseMetaFactory.create(request)
        );
    }
}
