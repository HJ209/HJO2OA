package com.hjo2oa.msg.ecosystem.interfaces;

import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordView;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationView;
import com.hjo2oa.msg.ecosystem.domain.IntegrationAvailabilityView;
import org.springframework.stereotype.Component;

@Component
public class EcosystemIntegrationDtoMapper {

    public EcosystemIntegrationDtos.IntegrationResponse toIntegrationResponse(EcosystemIntegrationView view) {
        return new EcosystemIntegrationDtos.IntegrationResponse(
                view.id(),
                view.integrationType(),
                view.displayName(),
                view.authMode(),
                view.callbackUrl(),
                view.signAlgorithm(),
                view.configRef(),
                view.status(),
                view.healthStatus(),
                view.lastCheckAt(),
                view.lastErrorSummary(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public EcosystemIntegrationDtos.AvailabilityResponse toAvailabilityResponse(
            IntegrationAvailabilityView view
    ) {
        return new EcosystemIntegrationDtos.AvailabilityResponse(
                view.id(),
                view.integrationType(),
                view.configRef(),
                view.healthStatus(),
                view.available()
        );
    }

    public EcosystemIntegrationDtos.CallbackAuditResponse toCallbackAuditResponse(CallbackAuditRecordView view) {
        return new EcosystemIntegrationDtos.CallbackAuditResponse(
                view.id(),
                view.integrationId(),
                view.callbackType(),
                view.verifyResult(),
                view.payloadSummary(),
                view.errorMessage(),
                view.idempotencyKey(),
                view.payloadDigest(),
                view.occurredAt()
        );
    }
}
