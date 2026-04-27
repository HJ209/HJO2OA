package com.hjo2oa.msg.ecosystem.interfaces;

import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationApplicationService;
import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationCommands;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/msg/ecosystem/callbacks")
public class EcosystemCallbackEndpoint {

    private final EcosystemIntegrationApplicationService applicationService;

    public EcosystemCallbackEndpoint(EcosystemIntegrationApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{tenantId}/{integrationId}/{callbackType}")
    public ResponseEntity<Map<String, Object>> acceptCallback(
            @PathVariable UUID tenantId,
            @PathVariable UUID integrationId,
            @PathVariable String callbackType,
            @RequestHeader("X-HJO2OA-Callback-Id") String idempotencyKey,
            @RequestHeader("X-HJO2OA-Signature") String signature,
            @RequestBody(required = false) String payload
    ) {
        var command = new EcosystemIntegrationCommands.VerifyCallbackCommand(
                tenantId,
                integrationId,
                callbackType,
                idempotencyKey,
                signature,
                payload
        );
        var audit = applicationService.verifyCallback(command);
        return ResponseEntity.ok(Map.of(
                "accepted",
                true,
                "auditId",
                audit.id(),
                "verifyResult",
                audit.verifyResult()
        ));
    }
}
