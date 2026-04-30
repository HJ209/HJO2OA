package com.hjo2oa.msg.mobile.support.interfaces;

import com.hjo2oa.msg.mobile.support.application.MobileSupportApplicationService;
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
@RequestMapping("/api/v1/msg/mobile-support")
public class MobileSupportController {

    private final MobileSupportApplicationService applicationService;
    private final MobileSupportDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public MobileSupportController(
            MobileSupportApplicationService applicationService,
            MobileSupportDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/devices/bind")
    public ApiResponse<MobileSupportDtos.DeviceBindingResponse> bindDevice(
            @Valid @RequestBody MobileSupportDtos.BindDeviceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.bindDevice(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/devices/{deviceId}/push-token")
    public ApiResponse<MobileSupportDtos.DeviceBindingResponse> updatePushToken(
            @PathVariable String deviceId,
            @Valid @RequestBody MobileSupportDtos.UpdatePushTokenRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updatePushToken(body.toCommand(deviceId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/devices")
    public ApiResponse<List<MobileSupportDtos.DeviceBindingResponse>> activeDevices(
            @RequestParam UUID tenantId,
            @RequestParam UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.activeDevices(tenantId, personId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/sessions")
    public ApiResponse<MobileSupportDtos.MobileSessionResponse> createSession(
            @Valid @RequestBody MobileSupportDtos.CreateSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createSession(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/sessions/{sessionId}/refresh")
    public ApiResponse<MobileSupportDtos.MobileSessionResponse> refreshSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MobileSupportDtos.RefreshSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.refreshSession(body.toCommand(sessionId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/sessions/{sessionId}/identity-snapshot")
    public ApiResponse<MobileSupportDtos.MobileSessionResponse> updateIdentitySnapshot(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MobileSupportDtos.UpdateSessionIdentitySnapshotRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateSessionIdentitySnapshot(body.toCommand(sessionId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/session/current")
    public ApiResponse<MobileSupportDtos.MobileSessionResponse> currentSession(
            @RequestParam UUID tenantId,
            @RequestParam UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.currentSession(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/admin/devices/{deviceId}/revoke")
    public ApiResponse<MobileSupportDtos.DeviceBindingResponse> revokeDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody MobileSupportDtos.RevokeDeviceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.revokeDevice(body.toCommand(deviceId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/admin/sessions/{sessionId}/freeze")
    public ApiResponse<MobileSupportDtos.MobileSessionResponse> freezeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody MobileSupportDtos.FreezeSessionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.freezeSession(body.toCommand(sessionId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/push-preferences")
    public ApiResponse<MobileSupportDtos.PushPreferenceResponse> pushPreference(
            @RequestParam UUID tenantId,
            @RequestParam UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.pushPreference(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/push-preferences")
    public ApiResponse<MobileSupportDtos.PushPreferenceResponse> savePushPreference(
            @Valid @RequestBody MobileSupportDtos.SavePushPreferenceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.savePushPreference(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }
}
