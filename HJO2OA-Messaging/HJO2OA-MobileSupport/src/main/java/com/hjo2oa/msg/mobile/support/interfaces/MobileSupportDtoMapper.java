package com.hjo2oa.msg.mobile.support.interfaces;

import com.hjo2oa.msg.mobile.support.domain.DeviceBindingView;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionView;
import org.springframework.stereotype.Component;

@Component
public class MobileSupportDtoMapper {

    public MobileSupportDtos.DeviceBindingResponse toResponse(DeviceBindingView view) {
        return new MobileSupportDtos.DeviceBindingResponse(
                view.id(),
                view.personId(),
                view.accountId(),
                view.deviceId(),
                view.deviceFingerprint(),
                view.platform(),
                view.appType(),
                view.pushToken(),
                view.bindStatus(),
                view.riskLevel(),
                view.lastLoginAt(),
                view.lastSeenAt(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public MobileSupportDtos.MobileSessionResponse toResponse(MobileSessionView view) {
        return new MobileSupportDtos.MobileSessionResponse(
                view.id(),
                view.deviceBindingId(),
                view.personId(),
                view.accountId(),
                view.currentAssignmentId(),
                view.currentPositionId(),
                view.sessionStatus(),
                view.riskLevelSnapshot(),
                view.riskFrozenAt(),
                view.riskReason(),
                view.issuedAt(),
                view.expiresAt(),
                view.lastHeartbeatAt(),
                view.refreshVersion(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
