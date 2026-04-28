package com.hjo2oa.msg.ecosystem.interfaces;

import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationApplicationService;
import com.hjo2oa.msg.ecosystem.application.EcosystemIntegrationCommands;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/msg/ecosystem/callbacks")
public class EcosystemCallbackEndpoint {

    private final EcosystemIntegrationApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public EcosystemCallbackEndpoint(
            EcosystemIntegrationApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/{tenantId}/{integrationId}/{callbackType}")
    public ApiResponse<Map<String, Object>> acceptCallback(
            @PathVariable UUID tenantId,
            @PathVariable UUID integrationId,
            @PathVariable String callbackType,
            @RequestHeader("X-HJO2OA-Callback-Id") String idempotencyKey,
            @RequestHeader("X-HJO2OA-Signature") String signature,
            @RequestBody(required = false) String payload,
            HttpServletRequest request
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
        return ApiResponse.success(
                Map.of(
                        "accepted",
                        true,
                        "auditId",
                        audit.id(),
                        "verifyResult",
                        audit.verifyResult()
                ),
                responseMetaFactory.create(request)
        );
    }
}
